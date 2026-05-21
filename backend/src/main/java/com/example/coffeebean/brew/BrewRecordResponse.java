package com.example.coffeebean.brew;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BrewRecordResponse {

    private Long id;

    private Long coffeeBeanId;

    private String brewMethod;

    private BigDecimal beanAmountGrams;

    private BigDecimal waterAmountMl;

    private String ratio;

    private BigDecimal waterTemperature;

    private String grindSize;

    private Integer brewTimeSeconds;

    private String resultSummary;

    private String resultNotes;

    private Boolean isRecommended;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
