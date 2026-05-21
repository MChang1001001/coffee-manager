package com.example.coffeebean.brew;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("brew_records")
public class BrewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

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

    private Integer isRecommended;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
