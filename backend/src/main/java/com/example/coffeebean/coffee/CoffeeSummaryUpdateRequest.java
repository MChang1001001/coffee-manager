package com.example.coffeebean.coffee;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CoffeeSummaryUpdateRequest {

    @Size(max = 128, message = "一句话总结不能超过128个字符")
    private String summaryTitle;

    private String flavorSummary;

    private String brewSuggestion;

    @Size(max = 32, message = "回购意向不能超过32个字符")
    private String repurchaseIntention;

    private String summaryText;

    @Size(max = 16, message = "总结来源不能超过16个字符")
    private String summarySource;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime summaryGeneratedAt;
}
