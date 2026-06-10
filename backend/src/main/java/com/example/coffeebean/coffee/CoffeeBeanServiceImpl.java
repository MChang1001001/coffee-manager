package com.example.coffeebean.coffee;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coffeebean.brew.BrewRecord;
import com.example.coffeebean.brew.BrewRecordMapper;
import com.example.coffeebean.brew.BrewRecordSummary;
import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.common.PageResponse;
import com.example.coffeebean.review.CoffeeReview;
import com.example.coffeebean.review.CoffeeReviewMapper;
import com.example.coffeebean.review.CoffeeReviewSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CoffeeBeanServiceImpl extends ServiceImpl<CoffeeBeanMapper, CoffeeBean> implements CoffeeBeanService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final String DEFAULT_STATUS = "UNOPENED";
    private static final int AI_CONTEXT_RECORD_LIMIT = 20;
    private static final Set<String> SUMMARY_SOURCE_OPTIONS = Set.of("MANUAL", "AI");
    private static final Set<String> REPURCHASE_OPTIONS = Set.of("未决定", "会回购", "看情况", "不回购");

    private final CoffeeReviewMapper coffeeReviewMapper;
    private final BrewRecordMapper brewRecordMapper;
    private final CoffeeAiSummaryClient coffeeAiSummaryClient;
    private final ObjectMapper objectMapper;

    public CoffeeBeanServiceImpl(
            CoffeeReviewMapper coffeeReviewMapper,
            BrewRecordMapper brewRecordMapper,
            CoffeeAiSummaryClient coffeeAiSummaryClient,
            ObjectMapper objectMapper) {
        this.coffeeReviewMapper = coffeeReviewMapper;
        this.brewRecordMapper = brewRecordMapper;
        this.coffeeAiSummaryClient = coffeeAiSummaryClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CoffeeBeanIdResponse create(Long userId, CoffeeBeanCreateRequest request) {
        CoffeeBean coffeeBean = new CoffeeBean();
        coffeeBean.setUserId(userId);
        coffeeBean.setName(requireText(request.getName(), "咖啡豆名称不能为空"));
        fillEditableFields(coffeeBean, request);
        coffeeBean.setCurrency(defaultIfBlank(request.getCurrency(), DEFAULT_CURRENCY));
        coffeeBean.setStatus(defaultIfBlank(request.getStatus(), DEFAULT_STATUS));
        coffeeBean.setReviewCount(0);
        coffeeBean.setBrewCount(0);
        coffeeBean.setDeleted(0);
        save(coffeeBean);
        return new CoffeeBeanIdResponse(coffeeBean.getId());
    }

    @Override
    public boolean update(Long userId, Long id, CoffeeBeanUpdateRequest request) {
        CoffeeBean coffeeBean = findOwnedBean(userId, id);
        coffeeBean.setName(requireText(request.getName(), "咖啡豆名称不能为空"));
        fillEditableFields(coffeeBean, request);
        coffeeBean.setCurrency(defaultIfBlank(
                request.getCurrency(),
                coffeeBean.getCurrency() == null ? DEFAULT_CURRENCY : coffeeBean.getCurrency()));
        coffeeBean.setStatus(defaultIfBlank(
                request.getStatus(),
                coffeeBean.getStatus() == null ? DEFAULT_STATUS : coffeeBean.getStatus()));
        return update(coffeeBean, new LambdaUpdateWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, id)
                .eq(CoffeeBean::getUserId, userId));
    }

    @Override
    public boolean delete(Long userId, Long id) {
        boolean removed = remove(new LambdaQueryWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, id)
                .eq(CoffeeBean::getUserId, userId));
        if (!removed) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "咖啡豆不存在");
        }
        return true;
    }

    @Override
    public CoffeeBeanDetailResponse getDetail(Long userId, Long id) {
        CoffeeBean coffeeBean = findOwnedBean(userId, id);
        CoffeeRecordSummary summary = loadRecordSummaries(userId, List.of(id))
                .getOrDefault(id, CoffeeRecordSummary.empty());
        return toDetailResponse(coffeeBean, summary);
    }

    @Override
    public PageResponse<CoffeeBeanListItemResponse> list(Long userId, CoffeeBeanListQuery query) {
        Page<CoffeeBean> pageRequest = Page.of(query.resolvedPage(), query.resolvedPageSize());
        Page<CoffeeBean> pageResult = page(pageRequest, buildListWrapper(userId, query));
        Map<Long, CoffeeRecordSummary> summaries = loadRecordSummaries(userId, pageResult.getRecords()
                .stream()
                .map(CoffeeBean::getId)
                .toList());
        List<CoffeeBeanListItemResponse> items = pageResult.getRecords()
                .stream()
                .map(coffeeBean -> toListItemResponse(
                        coffeeBean,
                        summaries.getOrDefault(coffeeBean.getId(), CoffeeRecordSummary.empty())))
                .toList();
        return PageResponse.of(items, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    public CoffeeSummaryDraftResponse generateAiSummary(Long userId, Long id) {
        CoffeeBean coffeeBean = findOwnedBean(userId, id);
        List<CoffeeReview> reviews = loadAiContextReviews(userId, id);
        List<BrewRecord> brewRecords = loadAiContextBrewRecords(userId, id);
        return coffeeAiSummaryClient.generateSummary(buildAiSummaryPrompt(coffeeBean, reviews, brewRecords));
    }

    @Override
    @Transactional
    public boolean updateSummary(Long userId, Long id, CoffeeSummaryUpdateRequest request) {
        findOwnedBean(userId, id);

        String summarySource = normalizeSummarySource(request.getSummarySource());
        boolean updated = update(new LambdaUpdateWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, id)
                .eq(CoffeeBean::getUserId, userId)
                .eq(CoffeeBean::getDeleted, 0)
                .set(CoffeeBean::getSummaryTitle, normalize(request.getSummaryTitle()))
                .set(CoffeeBean::getFlavorSummary, normalize(request.getFlavorSummary()))
                .set(CoffeeBean::getBrewSuggestion, normalize(request.getBrewSuggestion()))
                .set(CoffeeBean::getRepurchaseIntention, normalizeRepurchaseIntention(
                        request.getRepurchaseIntention()))
                .set(CoffeeBean::getSummaryText, normalize(request.getSummaryText()))
                .set(CoffeeBean::getSummarySource, summarySource)
                .set(CoffeeBean::getSummaryGeneratedAt, "AI".equals(summarySource) ? LocalDateTime.now() : null));
        if (!updated) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "咖啡豆不存在");
        }
        return true;
    }

    @Override
    @Transactional
    public void refreshReviewAggregates(Long coffeeBeanId, Long userId) {
        CoffeeReviewSummary summary = coffeeReviewMapper.selectSummaries(userId, List.of(coffeeBeanId))
                .stream()
                .findFirst()
                .orElse(null);

        update(new LambdaUpdateWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, coffeeBeanId)
                .eq(CoffeeBean::getUserId, userId)
                .eq(CoffeeBean::getDeleted, 0)
                .set(CoffeeBean::getReviewCount, toCount(summary == null ? null : summary.getReviewCount()))
                .set(CoffeeBean::getOverallRating, normalizeRating(
                        summary == null ? null : summary.getOverallRating())));
    }

    @Override
    @Transactional
    public void refreshBrewAggregates(Long coffeeBeanId, Long userId) {
        BrewRecordSummary summary = brewRecordMapper.selectSummaries(userId, List.of(coffeeBeanId))
                .stream()
                .findFirst()
                .orElse(null);

        update(new LambdaUpdateWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, coffeeBeanId)
                .eq(CoffeeBean::getUserId, userId)
                .eq(CoffeeBean::getDeleted, 0)
                .set(CoffeeBean::getBrewCount, toCount(summary == null ? null : summary.getBrewCount())));
    }

    private LambdaQueryWrapper<CoffeeBean> buildListWrapper(Long userId, CoffeeBeanListQuery query) {
        LambdaQueryWrapper<CoffeeBean> wrapper = new LambdaQueryWrapper<CoffeeBean>()
                .eq(CoffeeBean::getUserId, userId);

        String keyword = normalize(query.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(keywordWrapper -> keywordWrapper
                    .like(CoffeeBean::getName, keyword)
                    .or()
                    .like(CoffeeBean::getRoaster, keyword)
                    .or()
                    .like(CoffeeBean::getOrigin, keyword));
        }

        String roastLevel = normalize(query.getRoastLevel());
        if (StringUtils.hasText(roastLevel)) {
            wrapper.eq(CoffeeBean::getRoastLevel, roastLevel);
        }

        String processMethod = normalize(query.getProcessMethod());
        if (StringUtils.hasText(processMethod)) {
            wrapper.eq(CoffeeBean::getProcessMethod, processMethod);
        }

        String origin = normalize(query.getOrigin());
        if (StringUtils.hasText(origin)) {
            wrapper.eq(CoffeeBean::getOrigin, origin);
        }

        String status = normalize(query.getStatus());
        if (StringUtils.hasText(status)) {
            wrapper.eq(CoffeeBean::getStatus, status);
        }

        applyDrinkStatus(wrapper, CoffeeDrinkStatus.fromQueryValue(query.getDrinkStatus()));

        return wrapper
                .orderByDesc(CoffeeBean::getCreatedAt)
                .orderByDesc(CoffeeBean::getId);
    }

    private void applyDrinkStatus(LambdaQueryWrapper<CoffeeBean> wrapper, CoffeeDrinkStatus drinkStatus) {
        if (drinkStatus == null) {
            return;
        }

        switch (drinkStatus) {
            case NO_DATE -> wrapper.and(statusWrapper -> statusWrapper
                    .isNull(CoffeeBean::getBestFromDate)
                    .or()
                    .isNull(CoffeeBean::getBestToDate));
            case RESTING -> wrapper
                    .isNotNull(CoffeeBean::getBestFromDate)
                    .isNotNull(CoffeeBean::getBestToDate)
                    .apply("CURRENT_DATE < best_from_date");
            case EXPIRED -> wrapper
                    .isNotNull(CoffeeBean::getBestFromDate)
                    .isNotNull(CoffeeBean::getBestToDate)
                    .apply("CURRENT_DATE > best_to_date");
            case EXPIRING_SOON -> wrapper
                    .isNotNull(CoffeeBean::getBestFromDate)
                    .isNotNull(CoffeeBean::getBestToDate)
                    .apply("CURRENT_DATE >= best_from_date")
                    .apply("CURRENT_DATE <= best_to_date")
                    .apply("DATEDIFF(best_to_date, CURRENT_DATE) <= 7");
            case READY -> wrapper
                    .isNotNull(CoffeeBean::getBestFromDate)
                    .isNotNull(CoffeeBean::getBestToDate)
                    .apply("CURRENT_DATE >= best_from_date")
                    .apply("CURRENT_DATE <= best_to_date")
                    .apply("DATEDIFF(best_to_date, CURRENT_DATE) > 7");
        }
    }

    private CoffeeBean findOwnedBean(Long userId, Long id) {
        CoffeeBean coffeeBean = getOne(new LambdaQueryWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, id)
                .eq(CoffeeBean::getUserId, userId)
                .last("LIMIT 1"));
        if (coffeeBean == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "咖啡豆不存在");
        }
        return coffeeBean;
    }

    private void fillEditableFields(CoffeeBean coffeeBean, CoffeeBeanCreateRequest request) {
        coffeeBean.setOrigin(normalize(request.getOrigin()));
        coffeeBean.setRegion(normalize(request.getRegion()));
        coffeeBean.setFarm(normalize(request.getFarm()));
        coffeeBean.setVariety(normalize(request.getVariety()));
        coffeeBean.setProcessMethod(normalize(request.getProcessMethod()));
        coffeeBean.setRoastLevel(normalize(request.getRoastLevel()));
        coffeeBean.setRoaster(normalize(request.getRoaster()));
        coffeeBean.setRoastDate(request.getRoastDate());
        coffeeBean.setBestFromDate(request.getBestFromDate());
        coffeeBean.setBestToDate(request.getBestToDate());
        coffeeBean.setPurchaseDate(request.getPurchaseDate());
        coffeeBean.setOpenDate(request.getOpenDate());
        coffeeBean.setFinishDate(request.getFinishDate());
        coffeeBean.setNetWeightGrams(request.getNetWeightGrams());
        coffeeBean.setPrice(request.getPrice());
        coffeeBean.setCoverImageUrl(normalize(request.getCoverImageUrl()));
        coffeeBean.setNotes(normalize(request.getNotes()));
    }

    private void fillEditableFields(CoffeeBean coffeeBean, CoffeeBeanUpdateRequest request) {
        coffeeBean.setOrigin(normalize(request.getOrigin()));
        coffeeBean.setRegion(normalize(request.getRegion()));
        coffeeBean.setFarm(normalize(request.getFarm()));
        coffeeBean.setVariety(normalize(request.getVariety()));
        coffeeBean.setProcessMethod(normalize(request.getProcessMethod()));
        coffeeBean.setRoastLevel(normalize(request.getRoastLevel()));
        coffeeBean.setRoaster(normalize(request.getRoaster()));
        coffeeBean.setRoastDate(request.getRoastDate());
        coffeeBean.setBestFromDate(request.getBestFromDate());
        coffeeBean.setBestToDate(request.getBestToDate());
        coffeeBean.setPurchaseDate(request.getPurchaseDate());
        coffeeBean.setOpenDate(request.getOpenDate());
        coffeeBean.setFinishDate(request.getFinishDate());
        coffeeBean.setNetWeightGrams(request.getNetWeightGrams());
        coffeeBean.setPrice(request.getPrice());
        coffeeBean.setCoverImageUrl(normalize(request.getCoverImageUrl()));
        coffeeBean.setNotes(normalize(request.getNotes()));
    }

    private List<CoffeeReview> loadAiContextReviews(Long userId, Long coffeeBeanId) {
        return coffeeReviewMapper.selectList(new LambdaQueryWrapper<CoffeeReview>()
                .eq(CoffeeReview::getUserId, userId)
                .eq(CoffeeReview::getCoffeeBeanId, coffeeBeanId)
                .orderByDesc(CoffeeReview::getCreatedAt)
                .orderByDesc(CoffeeReview::getId)
                .last("LIMIT " + AI_CONTEXT_RECORD_LIMIT));
    }

    private List<BrewRecord> loadAiContextBrewRecords(Long userId, Long coffeeBeanId) {
        return brewRecordMapper.selectList(new LambdaQueryWrapper<BrewRecord>()
                .eq(BrewRecord::getUserId, userId)
                .eq(BrewRecord::getCoffeeBeanId, coffeeBeanId)
                .orderByDesc(BrewRecord::getCreatedAt)
                .orderByDesc(BrewRecord::getId)
                .last("LIMIT " + AI_CONTEXT_RECORD_LIMIT));
    }

    private String buildAiSummaryPrompt(
            CoffeeBean coffeeBean,
            List<CoffeeReview> reviews,
            List<BrewRecord> brewRecords) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("coffeeBean", toPromptCoffeeBean(coffeeBean));
        data.put("reviews", reviews.stream().map(this::toPromptReview).toList());
        data.put("brewRecords", brewRecords.stream().map(this::toPromptBrewRecord).toList());
        data.put("reviewRecordCount", reviews.size());
        data.put("brewRecordCount", brewRecords.size());

        try {
            return """
                    请根据下面 JSON 数据生成咖啡豆评测总结草稿。请只输出 JSON，不要 Markdown。
                    如果评价或冲煮记录较少，请自然说明“记录较少，结论偏初步”。

                    数据：
                    %s
                    """.formatted(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 总结上下文构造失败，请稍后重试。");
        }
    }

    private Map<String, Object> toPromptCoffeeBean(CoffeeBean coffeeBean) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", coffeeBean.getName());
        data.put("origin", coffeeBean.getOrigin());
        data.put("region", coffeeBean.getRegion());
        data.put("farm", coffeeBean.getFarm());
        data.put("variety", coffeeBean.getVariety());
        data.put("processMethod", coffeeBean.getProcessMethod());
        data.put("roastLevel", coffeeBean.getRoastLevel());
        data.put("roaster", coffeeBean.getRoaster());
        data.put("roastDate", dateText(coffeeBean.getRoastDate()));
        data.put("bestFromDate", dateText(coffeeBean.getBestFromDate()));
        data.put("bestToDate", dateText(coffeeBean.getBestToDate()));
        data.put("purchaseDate", dateText(coffeeBean.getPurchaseDate()));
        data.put("openDate", dateText(coffeeBean.getOpenDate()));
        data.put("finishDate", dateText(coffeeBean.getFinishDate()));
        data.put("netWeightGrams", coffeeBean.getNetWeightGrams());
        data.put("price", coffeeBean.getPrice());
        data.put("currency", coffeeBean.getCurrency());
        data.put("status", coffeeBean.getStatus());
        data.put("overallRating", coffeeBean.getOverallRating());
        data.put("reviewCount", coffeeBean.getReviewCount());
        data.put("brewCount", coffeeBean.getBrewCount());
        data.put("notes", coffeeBean.getNotes());
        return data;
    }

    private Map<String, Object> toPromptReview(CoffeeReview review) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overallRating", review.getOverallRating());
        data.put("aromaRating", review.getAromaRating());
        data.put("acidityRating", review.getAcidityRating());
        data.put("sweetnessRating", review.getSweetnessRating());
        data.put("bitternessRating", review.getBitternessRating());
        data.put("bodyRating", review.getBodyRating());
        data.put("aftertasteRating", review.getAftertasteRating());
        data.put("content", review.getContent());
        data.put("createdAt", dateTimeText(review.getCreatedAt()));
        return data;
    }

    private Map<String, Object> toPromptBrewRecord(BrewRecord brewRecord) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("brewMethod", brewRecord.getBrewMethod());
        data.put("beanAmountGrams", brewRecord.getBeanAmountGrams());
        data.put("waterAmountMl", brewRecord.getWaterAmountMl());
        data.put("ratio", brewRecord.getRatio());
        data.put("waterTemperature", brewRecord.getWaterTemperature());
        data.put("grindSize", brewRecord.getGrindSize());
        data.put("brewTimeSeconds", brewRecord.getBrewTimeSeconds());
        data.put("resultSummary", brewRecord.getResultSummary());
        data.put("resultNotes", brewRecord.getResultNotes());
        data.put("isRecommended", brewRecord.getIsRecommended() != null && brewRecord.getIsRecommended() == 1);
        data.put("createdAt", dateTimeText(brewRecord.getCreatedAt()));
        return data;
    }

    private Map<Long, CoffeeRecordSummary> loadRecordSummaries(Long userId, List<Long> coffeeBeanIds) {
        if (coffeeBeanIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, CoffeeReviewSummary> reviewSummaries = coffeeReviewMapper.selectSummaries(userId, coffeeBeanIds)
                .stream()
                .collect(Collectors.toMap(CoffeeReviewSummary::getCoffeeBeanId, Function.identity()));
        Map<Long, BrewRecordSummary> brewSummaries = brewRecordMapper.selectSummaries(userId, coffeeBeanIds)
                .stream()
                .collect(Collectors.toMap(BrewRecordSummary::getCoffeeBeanId, Function.identity()));

        return coffeeBeanIds.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), coffeeBeanId -> {
                    CoffeeReviewSummary reviewSummary = reviewSummaries.get(coffeeBeanId);
                    BrewRecordSummary brewSummary = brewSummaries.get(coffeeBeanId);

                    return new CoffeeRecordSummary(
                            normalizeRating(reviewSummary == null ? null : reviewSummary.getOverallRating()),
                            toCount(reviewSummary == null ? null : reviewSummary.getReviewCount()),
                            toCount(brewSummary == null ? null : brewSummary.getBrewCount()));
                }));
    }

    private CoffeeBeanListItemResponse toListItemResponse(CoffeeBean coffeeBean, CoffeeRecordSummary summary) {
        CoffeeBeanListItemResponse response = new CoffeeBeanListItemResponse();
        response.setId(coffeeBean.getId());
        response.setName(coffeeBean.getName());
        response.setOrigin(coffeeBean.getOrigin());
        response.setRegion(coffeeBean.getRegion());
        response.setVariety(coffeeBean.getVariety());
        response.setProcessMethod(coffeeBean.getProcessMethod());
        response.setRoastLevel(coffeeBean.getRoastLevel());
        response.setRoaster(coffeeBean.getRoaster());
        response.setRoastDate(coffeeBean.getRoastDate());
        response.setBestFromDate(coffeeBean.getBestFromDate());
        response.setBestToDate(coffeeBean.getBestToDate());
        response.setPurchaseDate(coffeeBean.getPurchaseDate());
        response.setStatus(coffeeBean.getStatus());
        response.setCoverImageUrl(coffeeBean.getCoverImageUrl());
        response.setOverallRating(summary.overallRating());
        response.setReviewCount(summary.reviewCount());
        response.setBrewCount(summary.brewCount());
        response.setSummaryTitle(coffeeBean.getSummaryTitle());
        response.setCreatedAt(coffeeBean.getCreatedAt());
        return response;
    }

    private CoffeeBeanDetailResponse toDetailResponse(CoffeeBean coffeeBean, CoffeeRecordSummary summary) {
        CoffeeBeanDetailResponse response = new CoffeeBeanDetailResponse();
        response.setId(coffeeBean.getId());
        response.setName(coffeeBean.getName());
        response.setOrigin(coffeeBean.getOrigin());
        response.setRegion(coffeeBean.getRegion());
        response.setFarm(coffeeBean.getFarm());
        response.setVariety(coffeeBean.getVariety());
        response.setProcessMethod(coffeeBean.getProcessMethod());
        response.setRoastLevel(coffeeBean.getRoastLevel());
        response.setRoaster(coffeeBean.getRoaster());
        response.setRoastDate(coffeeBean.getRoastDate());
        response.setBestFromDate(coffeeBean.getBestFromDate());
        response.setBestToDate(coffeeBean.getBestToDate());
        response.setPurchaseDate(coffeeBean.getPurchaseDate());
        response.setOpenDate(coffeeBean.getOpenDate());
        response.setFinishDate(coffeeBean.getFinishDate());
        response.setNetWeightGrams(coffeeBean.getNetWeightGrams());
        response.setPrice(coffeeBean.getPrice());
        response.setCurrency(coffeeBean.getCurrency());
        response.setStatus(coffeeBean.getStatus());
        response.setCoverImageUrl(coffeeBean.getCoverImageUrl());
        response.setOverallRating(summary.overallRating());
        response.setReviewCount(summary.reviewCount());
        response.setBrewCount(summary.brewCount());
        response.setSummaryTitle(coffeeBean.getSummaryTitle());
        response.setFlavorSummary(coffeeBean.getFlavorSummary());
        response.setBrewSuggestion(coffeeBean.getBrewSuggestion());
        response.setRepurchaseIntention(coffeeBean.getRepurchaseIntention());
        response.setSummaryText(coffeeBean.getSummaryText());
        response.setSummarySource(coffeeBean.getSummarySource());
        response.setSummaryGeneratedAt(coffeeBean.getSummaryGeneratedAt());
        response.setNotes(coffeeBean.getNotes());
        response.setCreatedAt(coffeeBean.getCreatedAt());
        response.setUpdatedAt(coffeeBean.getUpdatedAt());
        return response;
    }

    private BigDecimal normalizeRating(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private Integer toCount(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }

    private String normalizeSummarySource(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return "MANUAL";
        }

        String upperCaseValue = normalized.toUpperCase();
        if (!SUMMARY_SOURCE_OPTIONS.contains(upperCaseValue)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "总结来源只能是 MANUAL 或 AI");
        }
        return upperCaseValue;
    }

    private String normalizeRepurchaseIntention(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (!REPURCHASE_OPTIONS.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回购意向只能是：未决定 / 会回购 / 看情况 / 不回购");
        }
        return normalized;
    }

    private String dateText(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String dateTimeText(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String requireText(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return normalized;
    }

    private record CoffeeRecordSummary(BigDecimal overallRating, Integer reviewCount, Integer brewCount) {

        private static CoffeeRecordSummary empty() {
            return new CoffeeRecordSummary(null, 0, 0);
        }
    }
}
