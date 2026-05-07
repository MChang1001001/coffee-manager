package com.example.coffeebean.coffee;

import com.example.coffeebean.common.PageResponse;

public interface CoffeeBeanService {

    CoffeeBeanIdResponse create(Long userId, CoffeeBeanCreateRequest request);

    boolean update(Long userId, Long id, CoffeeBeanUpdateRequest request);

    boolean delete(Long userId, Long id);

    CoffeeBeanDetailResponse getDetail(Long userId, Long id);

    PageResponse<CoffeeBeanListItemResponse> list(Long userId, CoffeeBeanListQuery query);
}
