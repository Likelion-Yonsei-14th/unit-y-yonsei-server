package com.likelion.yonsei.daedongje.domain.performance.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceSetlistErrorCode implements ErrorCode {

    PERFORMANCE_SETLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PS-001", "Performance setlist not found.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PerformanceSetlistErrorCode(HttpStatus status, String code, String message) {
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
