package com.example.coffeebean.review;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CoffeeReviewUpdateRequest {

    @NotNull(message = "综合评分不能为空")
    @DecimalMin(value = "0.0", message = "综合评分不能小于0.0")
    @DecimalMax(value = "5.0", message = "综合评分不能大于5.0")
    private BigDecimal overallRating;

    @DecimalMin(value = "0.0", message = "香气评分不能小于0.0")
    @DecimalMax(value = "5.0", message = "香气评分不能大于5.0")
    private BigDecimal aromaRating;

    @DecimalMin(value = "0.0", message = "酸度评分不能小于0.0")
    @DecimalMax(value = "5.0", message = "酸度评分不能大于5.0")
    private BigDecimal acidityRating;

    @DecimalMin(value = "0.0", message = "甜感评分不能小于0.0")
    @DecimalMax(value = "5.0", message = "甜感评分不能大于5.0")
    private BigDecimal sweetnessRating;

    @DecimalMin(value = "0.0", message = "苦感评分不能小于0.0")
    @DecimalMax(value = "5.0", message = "苦感评分不能大于5.0")
    private BigDecimal bitternessRating;

    @DecimalMin(value = "0.0", message = "醇厚度评分不能小于0.0")
    @DecimalMax(value = "5.0", message = "醇厚度评分不能大于5.0")
    private BigDecimal bodyRating;

    @DecimalMin(value = "0.0", message = "余韵评分不能小于0.0")
    @DecimalMax(value = "5.0", message = "余韵评分不能大于5.0")
    private BigDecimal aftertasteRating;

    private String content;
}
