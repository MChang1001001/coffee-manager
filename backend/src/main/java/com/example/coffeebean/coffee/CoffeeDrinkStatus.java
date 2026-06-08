package com.example.coffeebean.coffee;

import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import java.util.Locale;
import org.springframework.util.StringUtils;

enum CoffeeDrinkStatus {
    NO_DATE,
    RESTING,
    READY,
    EXPIRING_SOON,
    EXPIRED;

    static CoffeeDrinkStatus fromQueryValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return CoffeeDrinkStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "饮用状态参数不正确");
        }
    }
}
