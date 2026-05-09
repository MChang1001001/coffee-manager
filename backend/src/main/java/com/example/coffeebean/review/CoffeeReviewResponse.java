package com.example.coffeebean.review;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CoffeeReviewResponse {

    private Long id;

    private Long coffeeBeanId;

    private BigDecimal overallRating;

    private BigDecimal aromaRating;

    private BigDecimal acidityRating;

    private BigDecimal sweetnessRating;

    private BigDecimal bitternessRating;

    private BigDecimal bodyRating;

    private BigDecimal aftertasteRating;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
