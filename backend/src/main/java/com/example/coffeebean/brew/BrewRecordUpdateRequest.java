package com.example.coffeebean.brew;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class BrewRecordUpdateRequest {

    @NotBlank(message = "冲煮方式不能为空")
    @Size(max = 64, message = "冲煮方式不能超过64个字符")
    private String brewMethod;

    @DecimalMin(value = "0.01", message = "粉量必须大于0")
    private BigDecimal beanAmountGrams;

    @DecimalMin(value = "0.01", message = "水量必须大于0")
    private BigDecimal waterAmountMl;

    @Size(max = 32, message = "比例不能超过32个字符")
    private String ratio;

    @DecimalMin(value = "0.01", message = "水温必须大于0")
    private BigDecimal waterTemperature;

    @Size(max = 128, message = "研磨度不能超过128个字符")
    private String grindSize;

    @Positive(message = "冲煮时长必须大于0")
    private Integer brewTimeSeconds;

    @Size(max = 255, message = "结果摘要不能超过255个字符")
    private String resultSummary;

    private String resultNotes;

    private Boolean isRecommended;
}
