package com.example.coffeebean.coffee;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CoffeeBeanDetailResponse {

    private Long id;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
