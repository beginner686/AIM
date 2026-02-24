package com.ailink.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PARAM_ERROR(40000, "request parameter error"),
    UNAUTHORIZED(40100, "unauthorized"),
    FORBIDDEN(40300, "forbidden"),
    NOT_FOUND(40400, "resource not found"),
    BUSINESS_ERROR(50001, "business error"),
    SYSTEM_ERROR(50000, "system error");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
