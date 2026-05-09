package com.example.coffeebean.review;

import com.example.coffeebean.common.PageResponse;

public interface CoffeeReviewService {

    CoffeeReviewIdResponse create(Long userId, Long coffeeBeanId, CoffeeReviewCreateRequest request);

    PageResponse<CoffeeReviewResponse> listByCoffeeBean(Long userId, Long coffeeBeanId, CoffeeReviewListQuery query);

    CoffeeReviewResponse getDetail(Long userId, Long id);

    boolean update(Long userId, Long id, CoffeeReviewUpdateRequest request);

    boolean delete(Long userId, Long id);
}
