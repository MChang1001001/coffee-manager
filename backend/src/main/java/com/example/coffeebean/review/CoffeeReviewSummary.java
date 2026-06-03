package com.example.coffeebean.review;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CoffeeReviewSummary {

    private Long coffeeBeanId;

    private BigDecimal overallRating;

    private Long reviewCount;
}
