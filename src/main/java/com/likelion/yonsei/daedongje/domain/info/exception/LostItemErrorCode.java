package com.likelion.yonsei.daedongje.domain.info.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum LostItemErrorCode implements ErrorCode {

    LOST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "I-002", "분실물을 찾을 수 없습니다."),
    LOST_ITEM_LOCATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "I-006", "존재하지 않는 위치(MapLocation)입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    LostItemErrorCode(HttpStatus status, String code, String message) {
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
