package com.example.coffeebean.coffee;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CoffeeBeanListQuery {

    @Size(max = 128, message = "关键词不能超过128个字符")
    private String keyword;

    @Size(max = 64, message = "烘焙度不能超过64个字符")
    private String roastLevel;

    @Size(max = 64, message = "处理法不能超过64个字符")
    private String processMethod;

    @Size(max = 128, message = "产地不能超过128个字符")
    private String origin;

    @Min(value = 1, message = "页码必须大于等于1")
    private Long page = 1L;

    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Long pageSize = 20L;

    public long resolvedPage() {
        return page == null ? 1L : page;
    }

    public long resolvedPageSize() {
        return pageSize == null ? 20L : pageSize;
    }
}
