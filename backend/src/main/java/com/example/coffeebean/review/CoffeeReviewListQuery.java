package com.example.coffeebean.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CoffeeReviewListQuery {

    @Min(value = 1, message = "page must be greater than or equal to 1")
    private Long page = 1L;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    private Long pageSize = 20L;

    public long resolvedPage() {
        return page == null ? 1L : page;
    }

    public long resolvedPageSize() {
        return pageSize == null ? 20L : pageSize;
    }
}
