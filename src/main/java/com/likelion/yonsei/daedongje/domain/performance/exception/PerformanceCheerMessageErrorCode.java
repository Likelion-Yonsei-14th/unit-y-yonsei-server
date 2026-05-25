package com.likelion.yonsei.daedongje.domain.performance.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceCheerMessageErrorCode implements ErrorCode {

    CHEER_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PCM-001", "존재하지 않는 응원 메시지입니다."),
    CHEER_MESSAGE_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "PCM-002", "응원 메시지 내용은 필수입니다."),
    CHEER_MESSAGE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "PCM-003", "응원 메시지는 255자를 넘을 수 없습니다."),
    CHEER_MESSAGE_PERFORMANCE_REQUIRED(HttpStatus.BAD_REQUEST, "PCM-004", "응원 메시지에는 공연 정보가 필요합니다."),
    CHEER_MESSAGE_SETLIST_FORBIDDEN(HttpStatus.FORBIDDEN, "PCM-005", "셋리스트가 해당 공연에 속하지 않습니다."),
    CHEER_MESSAGE_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "PCM-006", "응원 메시지 등록 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");

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
