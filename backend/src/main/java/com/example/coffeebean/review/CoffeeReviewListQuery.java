package com.example.coffeebean.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CoffeeReviewListQuery {

    @Min(value = 1, message = "页码必须大于等于1")
    private Long page = 1L;

    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Long pageSize = 20L;

    public long resolvedPage() {
        return page == null ? 1L : page;
    }

    public long resolvedPageSize() {
        return pageSize == null ? 20L : pageSize;
    }
}
