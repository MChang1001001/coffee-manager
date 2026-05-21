package com.example.coffeebean.brew;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class BrewRecordUpdateRequest {

    @NotBlank(message = "brewMethod cannot be blank")
    @Size(max = 64, message = "brewMethod length must be less than or equal to 64")
    private String brewMethod;

    @DecimalMin(value = "0.01", message = "beanAmountGrams must be greater than 0")
    private BigDecimal beanAmountGrams;

    @DecimalMin(value = "0.01", message = "waterAmountMl must be greater than 0")
    private BigDecimal waterAmountMl;

    @Size(max = 32, message = "ratio length must be less than or equal to 32")
    private String ratio;

    @DecimalMin(value = "0.01", message = "waterTemperature must be greater than 0")
    private BigDecimal waterTemperature;

    @Size(max = 128, message = "grindSize length must be less than or equal to 128")
    private String grindSize;

    @Positive(message = "brewTimeSeconds must be greater than 0")
    private Integer brewTimeSeconds;

    @Size(max = 255, message = "resultSummary length must be less than or equal to 255")
    private String resultSummary;

    private String resultNotes;

    private Boolean isRecommended;
}
