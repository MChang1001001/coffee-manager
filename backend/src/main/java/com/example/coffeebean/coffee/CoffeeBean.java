package com.example.coffeebean.coffee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coffee_beans")
public class CoffeeBean {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String origin;

    private String region;

    private String farm;

    private String variety;

    private String processMethod;

    private String roastLevel;

    private String roaster;

    private LocalDate roastDate;

    private LocalDate purchaseDate;

    private LocalDate openDate;

    private LocalDate finishDate;

    private BigDecimal netWeightGrams;

    private BigDecimal price;

    private String currency;

    private String status;

    private String coverImageUrl;

    private BigDecimal overallRating;

    private Integer reviewCount;

    private Integer brewCount;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
