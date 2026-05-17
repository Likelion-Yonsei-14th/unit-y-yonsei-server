package com.likelion.yonsei.daedongje.domain.performance.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceErrorCode implements ErrorCode {

    PERFORMANCE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "P-001", "Performance name is required."),
    PERFORMANCE_ADMIN_REQUIRED(HttpStatus.BAD_REQUEST, "P-002", "Performance admin account is required.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PerformanceErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
