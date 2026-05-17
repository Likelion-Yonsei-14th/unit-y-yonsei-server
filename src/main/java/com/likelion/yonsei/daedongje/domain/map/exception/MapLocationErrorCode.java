package com.likelion.yonsei.daedongje.domain.map.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MapLocationErrorCode implements ErrorCode {

    MAP_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "M-001", "존재하지 않는 지도 위치입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MapLocationErrorCode(HttpStatus status, String code, String message) {
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
