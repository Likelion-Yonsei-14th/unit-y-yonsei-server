package com.likelion.yonsei.daedongje.domain.info.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CreatorErrorCode implements ErrorCode {

    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "I-004", "만든이 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CreatorErrorCode(HttpStatus status, String code, String message) {
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
