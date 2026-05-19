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
    FORBIDDEN(HttpStatus.FORBIDDEN, "A-009", "접근 권한이 없습니다."),


    ADMIN_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A-010", "존재하지 않는 관리자 계정입니다."),
    SUPER_PASSWORD_RESET_FORBIDDEN(HttpStatus.FORBIDDEN, "A-011", "SUPER 계정의 비밀번호는 재설정할 수 없습니다."),
    SESSION_REPOSITORY_NOT_AVAILABLE(HttpStatus.INTERNAL_SERVER_ERROR, "A-012", "세션 저장소를 사용할 수 없습니다."),
    SUPER_ADMIN_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "A-013", "Super Admin 계정은 삭제할 수 없습니다."),
    ADMIN_HAS_OWNED_BOOTHS(HttpStatus.BAD_REQUEST, "A-014", "소유한 부스가 있어 어드민 계정을 삭제할 수 없습니다. 먼저 부스를 정리해주세요."),
    INVALID_BOOTH_OPERATING_DATE(HttpStatus.BAD_REQUEST, "A-015", "부스 운영 날짜는 1~3 사이의 숫자여야 합니다."),
    ADMIN_HAS_OWNED_PERFORMANCES(HttpStatus.BAD_REQUEST, "A-016", "소유한 공연이 있어 어드민 계정을 삭제할 수 없습니다. 먼저 공연을 정리해주세요."),
    INVALID_PERFORMANCE_NAME_LENGTH(HttpStatus.BAD_REQUEST, "A-017", "공연 이름은 100자 이하로 입력해주세요."),
    INVALID_PERFORMANCE_DATE(HttpStatus.BAD_REQUEST, "A-018", "공연 일자는 1~3 사이의 숫자여야 합니다."),
    INVALID_PERFORMANCE_TIME(HttpStatus.BAD_REQUEST, "A-019", "공연 시작 시간은 종료 시간보다 빨라야 합니다.");


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