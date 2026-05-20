package com.likelion.yonsei.daedongje.domain.performance.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceErrorCode implements ErrorCode {

    PERFORMANCE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "P-001", "Performance name is required."),
    PERFORMANCE_ADMIN_REQUIRED(HttpStatus.BAD_REQUEST, "P-002", "Performance admin account is required."),
    PERFORMANCE_CREATED_BY_REQUIRED(HttpStatus.BAD_REQUEST, "P-003", "Performance creator account is required."),
    PERFORMANCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "P-004", "Performance already exists for this admin account."),
    PERFORMANCE_ADMIN_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "P-005", "Performance admin account must have PERFORMER role."),
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P-006", "Performance not found."),
    PERFORMANCE_INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "P-007", "Performance end time must be after start time."),
    PERFORMANCE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "P-008", "Performance image not found."),
    PERFORMANCE_HAS_IMAGES(HttpStatus.BAD_REQUEST, "P-009", "이미지가 있어 공연을 삭제할 수 없습니다. 먼저 이미지를 정리해주세요."),
    PERFORMANCE_HAS_SETLISTS(HttpStatus.BAD_REQUEST, "P-010", "셋리스트가 있어 공연을 삭제할 수 없습니다. 먼저 셋리스트를 정리해주세요."),
    PERFORMANCE_HAS_NOTICES(HttpStatus.BAD_REQUEST, "P-011", "공지가 있어 공연을 삭제할 수 없습니다. 먼저 공지를 정리해주세요.");

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
