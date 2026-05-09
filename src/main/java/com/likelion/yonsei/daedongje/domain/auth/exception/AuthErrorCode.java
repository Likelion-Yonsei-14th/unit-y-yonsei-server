package com.likelion.yonsei.daedongje.domain.auth.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "A-001", "이미 사용 중인 로그인 아이디입니다."),
    INVALID_ADMIN_ROLE(HttpStatus.BAD_REQUEST, "A-002", "유효하지 않은 관리자 권한입니다."),
    BOOTH_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "A-003", "Booth 권한 계정은 부스 정보가 필요합니다."),
    PERFORMER_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "A-004", "Performer 권한 계정은 공연 정보가 필요합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
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