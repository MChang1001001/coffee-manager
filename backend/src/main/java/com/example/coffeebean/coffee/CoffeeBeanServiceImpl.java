package com.example.coffeebean.coffee;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coffeebean.brew.BrewRecordMapper;
import com.example.coffeebean.brew.BrewRecordSummary;
import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.common.PageResponse;
import com.example.coffeebean.review.CoffeeReviewMapper;
import com.example.coffeebean.review.CoffeeReviewSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CoffeeBeanServiceImpl extends ServiceImpl<CoffeeBeanMapper, CoffeeBean> implements CoffeeBeanService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final String DEFAULT_STATUS = "UNOPENED";

    private final CoffeeReviewMapper coffeeReviewMapper;
    private final BrewRecordMapper brewRecordMapper;

    public CoffeeBeanServiceImpl(CoffeeReviewMapper coffeeReviewMapper, BrewRecordMapper brewRecordMapper) {
        this.coffeeReviewMapper = coffeeReviewMapper;
        this.brewRecordMapper = brewRecordMapper;
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
