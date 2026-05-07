package com.example.coffeebean.coffee;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CoffeeBeanCreateRequest {

    @NotBlank(message = "咖啡豆名称不能为空")
    @Size(max = 128, message = "咖啡豆名称不能超过128个字符")
    private String name;

    @Size(max = 128, message = "产地不能超过128个字符")
    private String origin;

    @Size(max = 128, message = "产区不能超过128个字符")
    private String region;

    @Size(max = 128, message = "庄园/农场不能超过128个字符")
    private String farm;

    @Size(max = 128, message = "品种不能超过128个字符")
    private String variety;

    @Size(max = 64, message = "处理法不能超过64个字符")
    private String processMethod;

    @Size(max = 64, message = "烘焙度不能超过64个字符")
    private String roastLevel;

    @Size(max = 128, message = "烘焙商不能超过128个字符")
    private String roaster;

    private LocalDate roastDate;

    private LocalDate purchaseDate;

    private LocalDate openDate;

    private LocalDate finishDate;

    @DecimalMin(value = "0.00", inclusive = false, message = "净含量必须大于0")
    private BigDecimal netWeightGrams;

    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;

    @Size(max = 16, message = "币种不能超过16个字符")
    private String currency;

    @Size(max = 64, message = "状态不能超过64个字符")
    private String status;

    @Size(max = 500, message = "封面图片地址不能超过500个字符")
    private String coverImageUrl;

    private String notes;
}
