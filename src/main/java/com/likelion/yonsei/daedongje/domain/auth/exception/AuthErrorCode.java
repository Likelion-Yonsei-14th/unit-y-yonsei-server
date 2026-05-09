package com.likelion.yonsei.daedongje.domain.auth.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "A-001", "이미 사용 중인 로그인 아이디입니다."),
    INVALID_ADMIN_ROLE(HttpStatus.BAD_REQUEST, "A-002", "유효하지 않은 관리자 권한입니다."),
    BOOTH_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "A-003", "Booth 권한 계정은 부스 정보가 필요합니다."),
    PERFORMER_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "A-004", "Performer 권한 계정은 공연 정보가 필요합니다."),
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A-005", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INACTIVE_ADMIN_ACCOUNT(HttpStatus.FORBIDDEN, "A-006", "비활성화된 관리자 계정입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A-007", "로그인이 필요합니다."),
    INVALID_SESSION(HttpStatus.UNAUTHORIZED, "A-008", "로그인 정보가 유효하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A-009", "접근 권한이 없습니다.");


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