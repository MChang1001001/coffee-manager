package com.example.coffeebean.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coffeebean.coffee.CoffeeBean;
import com.example.coffeebean.coffee.CoffeeBeanMapper;
import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.common.PageResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CoffeeReviewServiceImpl extends ServiceImpl<CoffeeReviewMapper, CoffeeReview>
        implements CoffeeReviewService {

    private static final BigDecimal MIN_RATING = new BigDecimal("0.0");
    private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
    private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

    private final CoffeeBeanMapper coffeeBeanMapper;

    public CoffeeReviewServiceImpl(CoffeeBeanMapper coffeeBeanMapper) {
        this.coffeeBeanMapper = coffeeBeanMapper;
    }

    @Override
    public CoffeeReviewIdResponse create(Long userId, Long coffeeBeanId, CoffeeReviewCreateRequest request) {
        ensureOwnedCoffeeBean(userId, coffeeBeanId);

        CoffeeReview review = new CoffeeReview();
        review.setUserId(userId);
        review.setCoffeeBeanId(coffeeBeanId);
        fillCreateFields(review, request);
        review.setDeleted(0);
        save(review);
        return new CoffeeReviewIdResponse(review.getId());
    }

    @Override
    public PageResponse<CoffeeReviewResponse> listByCoffeeBean(
            Long userId,
            Long coffeeBeanId,
            CoffeeReviewListQuery query) {
        ensureOwnedCoffeeBean(userId, coffeeBeanId);

        Page<CoffeeReview> pageRequest = Page.of(query.resolvedPage(), query.resolvedPageSize());
        Page<CoffeeReview> pageResult = page(pageRequest, new LambdaQueryWrapper<CoffeeReview>()
                .eq(CoffeeReview::getUserId, userId)
                .eq(CoffeeReview::getCoffeeBeanId, coffeeBeanId)
                .orderByDesc(CoffeeReview::getCreatedAt)
                .orderByDesc(CoffeeReview::getId));
        List<CoffeeReviewResponse> items = pageResult.getRecords()
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(items, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    public CoffeeReviewResponse getDetail(Long userId, Long id) {
        return toResponse(findOwnedReview(userId, id));
    }

    @Override
    public boolean update(Long userId, Long id, CoffeeReviewUpdateRequest request) {
        findOwnedReview(userId, id);

        boolean updated = update(new LambdaUpdateWrapper<CoffeeReview>()
                .eq(CoffeeReview::getId, id)
                .eq(CoffeeReview::getUserId, userId)
                .set(CoffeeReview::getOverallRating, requireRating(request.getOverallRating(), "综合评分"))
                .set(CoffeeReview::getAromaRating, validateRating(request.getAromaRating(), "香气评分"))
                .set(CoffeeReview::getAcidityRating, validateRating(request.getAcidityRating(), "酸度评分"))
                .set(CoffeeReview::getSweetnessRating, validateRating(request.getSweetnessRating(), "甜感评分"))
                .set(CoffeeReview::getBitternessRating, validateRating(request.getBitternessRating(), "苦感评分"))
                .set(CoffeeReview::getBodyRating, validateRating(request.getBodyRating(), "醇厚度评分"))
                .set(CoffeeReview::getAftertasteRating, validateRating(request.getAftertasteRating(), "余韵评分"))
                .set(CoffeeReview::getContent, normalize(request.getContent())));
        if (!updated) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return true;
    }

    @Override
    public boolean delete(Long userId, Long id) {
        findOwnedReview(userId, id);

        boolean removed = remove(new LambdaQueryWrapper<CoffeeReview>()
                .eq(CoffeeReview::getId, id)
                .eq(CoffeeReview::getUserId, userId));
        if (!removed) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return true;
    }

    private void ensureOwnedCoffeeBean(Long userId, Long coffeeBeanId) {
        Long count = coffeeBeanMapper.selectCount(new LambdaQueryWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, coffeeBeanId)
                .eq(CoffeeBean::getUserId, userId));
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "咖啡豆不存在");
        }
    }

    private CoffeeReview findOwnedReview(Long userId, Long id) {
        CoffeeReview review = getOne(new LambdaQueryWrapper<CoffeeReview>()
                .eq(CoffeeReview::getId, id)
                .eq(CoffeeReview::getUserId, userId)
                .last("LIMIT 1"));
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return review;
    }

    private void fillCreateFields(CoffeeReview review, CoffeeReviewCreateRequest request) {
        review.setOverallRating(requireRating(request.getOverallRating(), "综合评分"));
        review.setAromaRating(validateRating(request.getAromaRating(), "香气评分"));
        review.setAcidityRating(validateRating(request.getAcidityRating(), "酸度评分"));
        review.setSweetnessRating(validateRating(request.getSweetnessRating(), "甜感评分"));
        review.setBitternessRating(validateRating(request.getBitternessRating(), "苦感评分"));
        review.setBodyRating(validateRating(request.getBodyRating(), "醇厚度评分"));
        review.setAftertasteRating(validateRating(request.getAftertasteRating(), "余韵评分"));
        review.setContent(normalize(request.getContent()));
    }

    private CoffeeReviewResponse toResponse(CoffeeReview review) {
        CoffeeReviewResponse response = new CoffeeReviewResponse();
        response.setId(review.getId());
        response.setCoffeeBeanId(review.getCoffeeBeanId());
        response.setOverallRating(review.getOverallRating());
        response.setAromaRating(review.getAromaRating());
        response.setAcidityRating(review.getAcidityRating());
        response.setSweetnessRating(review.getSweetnessRating());
        response.setBitternessRating(review.getBitternessRating());
        response.setBodyRating(review.getBodyRating());
        response.setAftertasteRating(review.getAftertasteRating());
        response.setContent(review.getContent());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }

    private BigDecimal requireRating(BigDecimal rating, String label) {
        if (rating == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "不能为空");
        }
        return validateRating(rating, label);
    }

    private BigDecimal validateRating(BigDecimal rating, String label) {
        if (rating == null) {
            return null;
        }
        if (rating.compareTo(MIN_RATING) < 0 || rating.compareTo(MAX_RATING) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "必须在0.0到5.0之间");
        }
        if (rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "必须以0.5为步进");
        }
        return rating;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
