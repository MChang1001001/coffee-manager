package com.example.coffeebean.enums;

import java.util.List;

public record CoffeeEnumOptionsResponse(
        List<EnumOptionResponse> roastLevels,
        List<EnumOptionResponse> processMethods,
        List<EnumOptionResponse> origins,
        List<EnumOptionResponse> varieties) {

    public static CoffeeEnumOptionsResponse defaults() {
        return new CoffeeEnumOptionsResponse(
                List.of(
                        new EnumOptionResponse("浅烘", "LIGHT"),
                        new EnumOptionResponse("中浅烘", "MEDIUM_LIGHT"),
                        new EnumOptionResponse("中烘", "MEDIUM"),
                        new EnumOptionResponse("中深烘", "MEDIUM_DARK"),
                        new EnumOptionResponse("深烘", "DARK"),
                        new EnumOptionResponse("未知", "UNKNOWN")),
                List.of(
                        new EnumOptionResponse("水洗", "水洗"),
                        new EnumOptionResponse("日晒", "日晒"),
                        new EnumOptionResponse("蜜处理", "蜜处理"),
                        new EnumOptionResponse("厌氧发酵", "厌氧发酵"),
                        new EnumOptionResponse("厌氧日晒", "厌氧日晒"),
                        new EnumOptionResponse("厌氧水洗", "厌氧水洗"),
                        new EnumOptionResponse("湿刨法", "湿刨法"),
                        new EnumOptionResponse("半水洗", "半水洗"),
                        new EnumOptionResponse("酒桶发酵", "酒桶发酵"),
                        new EnumOptionResponse("实验处理", "实验处理"),
                        new EnumOptionResponse("未知", "未知")),
                List.of(
                        new EnumOptionResponse("埃塞俄比亚", "埃塞俄比亚"),
                        new EnumOptionResponse("哥伦比亚", "哥伦比亚"),
                        new EnumOptionResponse("巴拿马", "巴拿马"),
                        new EnumOptionResponse("肯尼亚", "肯尼亚"),
                        new EnumOptionResponse("危地马拉", "危地马拉"),
                        new EnumOptionResponse("哥斯达黎加", "哥斯达黎加"),
                        new EnumOptionResponse("巴西", "巴西"),
                        new EnumOptionResponse("印尼", "印尼"),
                        new EnumOptionResponse("云南", "云南")),
                List.of(
                        new EnumOptionResponse("阿拉比卡", "阿拉比卡"),
                        new EnumOptionResponse("罗布斯塔", "罗布斯塔"),
                        new EnumOptionResponse("瑰夏", "瑰夏"),
                        new EnumOptionResponse("波旁", "波旁"),
                        new EnumOptionResponse("铁皮卡", "铁皮卡"),
                        new EnumOptionResponse("卡杜拉", "卡杜拉"),
                        new EnumOptionResponse("卡杜艾", "卡杜艾"),
                        new EnumOptionResponse("SL28", "SL28"),
                        new EnumOptionResponse("SL34", "SL34"),
                        new EnumOptionResponse("帕卡马拉", "帕卡马拉"),
                        new EnumOptionResponse("卡蒂姆", "卡蒂姆"),
                        new EnumOptionResponse("混合豆", "混合豆"),
                        new EnumOptionResponse("未知", "未知")));
    }
}
