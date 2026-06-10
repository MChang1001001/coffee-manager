package com.example.coffeebean.coffee;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CoffeeSummaryDraftResponse {

    private String summaryTitle;

    private String flavorSummary;

    private String brewSuggestion;

    private String repurchaseIntention;

    private String summaryText;

    private String summarySource;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime summaryGeneratedAt;
}
