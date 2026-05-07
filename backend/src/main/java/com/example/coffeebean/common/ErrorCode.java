package com.example.coffeebean.common;

public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "请求错误"),
    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40100, "未登录或 token 无效"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "数据冲突"),
    SYSTEM_ERROR(50000, "服务端错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
