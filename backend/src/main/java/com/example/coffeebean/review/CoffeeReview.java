package com.example.coffeebean.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coffee_reviews")
public class CoffeeReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coffeeBeanId;

    private BigDecimal overallRating;

    private BigDecimal aromaRating;

    private BigDecimal acidityRating;

    private BigDecimal sweetnessRating;

    private BigDecimal bitternessRating;

    private BigDecimal bodyRating;

    private BigDecimal aftertasteRating;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
