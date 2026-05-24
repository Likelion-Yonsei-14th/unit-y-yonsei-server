package com.likelion.yonsei.daedongje.domain.performance.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceErrorCode implements ErrorCode {

    PERFORMANCE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "P-001", "공연 이름은 필수입니다."),
    PERFORMANCE_ADMIN_REQUIRED(HttpStatus.BAD_REQUEST, "P-002", "공연 어드민 계정은 필수입니다."),
    PERFORMANCE_CREATED_BY_REQUIRED(HttpStatus.BAD_REQUEST, "P-003", "공연 생성자 계정은 필수입니다."),
    PERFORMANCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "P-004", "이 어드민 계정에 연결된 공연이 이미 존재합니다."),
    PERFORMANCE_ADMIN_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "P-005", "공연 어드민 계정은 PERFORMER 역할이어야 합니다."),
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P-006", "존재하지 않는 공연입니다."),
    PERFORMANCE_INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "P-007", "공연 종료 시간은 시작 시간보다 늦어야 합니다."),
    PERFORMANCE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "P-008", "존재하지 않는 공연 이미지입니다."),
    PERFORMANCE_HAS_IMAGES(HttpStatus.BAD_REQUEST, "P-009", "이미지가 있어 공연을 삭제할 수 없습니다. 먼저 이미지를 정리해주세요."),
    PERFORMANCE_HAS_SETLISTS(HttpStatus.BAD_REQUEST, "P-010", "셋리스트가 있어 공연을 삭제할 수 없습니다. 먼저 셋리스트를 정리해주세요."),
    PERFORMANCE_HAS_NOTICES(HttpStatus.BAD_REQUEST, "P-011", "공지가 있어 공연을 삭제할 수 없습니다. 먼저 공지를 정리해주세요."),
    PERFORMANCE_DELETE_CONFLICT(HttpStatus.CONFLICT, "P-012", "연결된 데이터가 있어 공연을 삭제할 수 없습니다."),
    PERFORMANCE_CONTROL_FIELD_FORBIDDEN(HttpStatus.FORBIDDEN, "P-013", "공연 상태와 공연 구분은 운영진만 수정할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PerformanceErrorCode(HttpStatus status, String code, String message) {
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
