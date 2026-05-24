package com.likelion.yonsei.daedongje.domain.monitoring.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MonitoringErrorCode implements ErrorCode {

    INVALID_WEBHOOK_SECRET(HttpStatus.UNAUTHORIZED, "MON-001", "유효하지 않은 웹훅 시크릿입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MonitoringErrorCode(HttpStatus status, String code, String message) {
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
