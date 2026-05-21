package com.example.coffeebean.brew;

import com.example.coffeebean.common.PageResponse;

public interface BrewRecordService {

    BrewRecordIdResponse create(Long userId, Long coffeeBeanId, BrewRecordCreateRequest request);

    PageResponse<BrewRecordResponse> listByCoffeeBean(Long userId, Long coffeeBeanId, BrewRecordListQuery query);

    BrewRecordResponse getDetail(Long userId, Long id);

    boolean update(Long userId, Long id, BrewRecordUpdateRequest request);

    boolean delete(Long userId, Long id);
}
