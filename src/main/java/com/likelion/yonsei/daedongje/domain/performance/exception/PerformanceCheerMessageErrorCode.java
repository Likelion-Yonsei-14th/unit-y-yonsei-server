package com.likelion.yonsei.daedongje.domain.performance.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceCheerMessageErrorCode implements ErrorCode {

    CHEER_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PCM-001", "Performance cheer message not found."),
    CHEER_MESSAGE_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "PCM-002", "Cheer message content is required."),
    CHEER_MESSAGE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "PCM-003", "Cheer message content must be 255 characters or less."),
    CHEER_MESSAGE_PERFORMANCE_REQUIRED(HttpStatus.BAD_REQUEST, "PCM-004", "Performance is required for cheer message."),
    CHEER_MESSAGE_SETLIST_FORBIDDEN(HttpStatus.FORBIDDEN, "PCM-005", "Setlist does not belong to the requested performance.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PerformanceCheerMessageErrorCode(HttpStatus status, String code, String message) {
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
