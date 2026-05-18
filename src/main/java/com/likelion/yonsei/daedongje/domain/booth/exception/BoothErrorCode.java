package com.likelion.yonsei.daedongje.domain.booth.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum BoothErrorCode implements ErrorCode {

    BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "B-001", "존재하지 않는 부스입니다."),
    INVALID_BOOTH_TIME(HttpStatus.BAD_REQUEST, "B-003", "운영 종료 시간은 시작 시간보다 늦어야 합니다."),
    DUPLICATE_BOOTH_NAME(HttpStatus.CONFLICT, "B-004", "이미 존재하는 부스 이름입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BoothErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() { return status; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
