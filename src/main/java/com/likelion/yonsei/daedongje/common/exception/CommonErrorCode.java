package com.likelion.yonsei.daedongje.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인 무관 공통 에러 코드.
 *
 * <p>도메인별 에러는 각 도메인 패키지에 별도 enum 으로 추가한다 (예: {@code BoothErrorCode}).
 * 본 enum 에는 입력 검증, 인증/권한, 일반적 not-found, 서버 오류 등 범용 케이스만 둔다.
 */
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON-003", "권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-004", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-005", "허용되지 않는 메서드입니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON-006", "요청이 충돌했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON-503", "일시적으로 서비스를 이용할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CommonErrorCode(HttpStatus status, String code, String message) {
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
