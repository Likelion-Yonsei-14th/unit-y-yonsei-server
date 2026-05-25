package com.likelion.yonsei.daedongje.domain.info.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum NoticeErrorCode implements ErrorCode {

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "I-001", "공지사항을 찾을 수 없습니다."),
    INVALID_NOTICE_IMAGE_DISPLAY_ORDER(HttpStatus.BAD_REQUEST, "I-005", "공지 이미지 순서는 중복될 수 없습니다."),
    NOTICE_PERFORMANCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "I-007", "존재하지 않는 공연입니다."),
    NOTICE_BOOTH_NOT_FOUND(HttpStatus.BAD_REQUEST, "I-008", "존재하지 않는 부스입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    NoticeErrorCode(HttpStatus status, String code, String message) {
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
