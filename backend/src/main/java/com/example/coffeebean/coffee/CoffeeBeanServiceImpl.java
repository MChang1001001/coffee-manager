package com.example.coffeebean.coffee;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.common.PageResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CoffeeBeanServiceImpl extends ServiceImpl<CoffeeBeanMapper, CoffeeBean> implements CoffeeBeanService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final String DEFAULT_STATUS = "UNOPENED";

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
        return toDetailResponse(findOwnedBean(userId, id));
    }

    @Override
    public PageResponse<CoffeeBeanListItemResponse> list(Long userId, CoffeeBeanListQuery query) {
        Page<CoffeeBean> pageRequest = Page.of(query.resolvedPage(), query.resolvedPageSize());
        Page<CoffeeBean> pageResult = page(pageRequest, buildListWrapper(userId, query));
        List<CoffeeBeanListItemResponse> items = pageResult.getRecords()
                .stream()
                .map(this::toListItemResponse)
                .toList();
        return PageResponse.of(items, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
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

        return wrapper
                .orderByDesc(CoffeeBean::getCreatedAt)
                .orderByDesc(CoffeeBean::getId);
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
        coffeeBean.setPurchaseDate(request.getPurchaseDate());
        coffeeBean.setOpenDate(request.getOpenDate());
        coffeeBean.setFinishDate(request.getFinishDate());
        coffeeBean.setNetWeightGrams(request.getNetWeightGrams());
        coffeeBean.setPrice(request.getPrice());
        coffeeBean.setCoverImageUrl(normalize(request.getCoverImageUrl()));
        coffeeBean.setNotes(normalize(request.getNotes()));
    }

    private CoffeeBeanListItemResponse toListItemResponse(CoffeeBean coffeeBean) {
        CoffeeBeanListItemResponse response = new CoffeeBeanListItemResponse();
        response.setId(coffeeBean.getId());
        response.setName(coffeeBean.getName());
        response.setOrigin(coffeeBean.getOrigin());
        response.setRegion(coffeeBean.getRegion());
        response.setProcessMethod(coffeeBean.getProcessMethod());
        response.setRoastLevel(coffeeBean.getRoastLevel());
        response.setRoaster(coffeeBean.getRoaster());
        response.setRoastDate(coffeeBean.getRoastDate());
        response.setPurchaseDate(coffeeBean.getPurchaseDate());
        response.setStatus(coffeeBean.getStatus());
        response.setCoverImageUrl(coffeeBean.getCoverImageUrl());
        response.setOverallRating(coffeeBean.getOverallRating());
        response.setReviewCount(coffeeBean.getReviewCount());
        response.setBrewCount(coffeeBean.getBrewCount());
        response.setCreatedAt(coffeeBean.getCreatedAt());
        return response;
    }

    private CoffeeBeanDetailResponse toDetailResponse(CoffeeBean coffeeBean) {
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
        response.setPurchaseDate(coffeeBean.getPurchaseDate());
        response.setOpenDate(coffeeBean.getOpenDate());
        response.setFinishDate(coffeeBean.getFinishDate());
        response.setNetWeightGrams(coffeeBean.getNetWeightGrams());
        response.setPrice(coffeeBean.getPrice());
        response.setCurrency(coffeeBean.getCurrency());
        response.setStatus(coffeeBean.getStatus());
        response.setCoverImageUrl(coffeeBean.getCoverImageUrl());
        response.setOverallRating(coffeeBean.getOverallRating());
        response.setReviewCount(coffeeBean.getReviewCount());
        response.setBrewCount(coffeeBean.getBrewCount());
        response.setNotes(coffeeBean.getNotes());
        response.setCreatedAt(coffeeBean.getCreatedAt());
        response.setUpdatedAt(coffeeBean.getUpdatedAt());
        return response;
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
}
