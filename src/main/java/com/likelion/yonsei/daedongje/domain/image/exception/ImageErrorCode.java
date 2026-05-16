package com.likelion.yonsei.daedongje.domain.image.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ImageErrorCode implements ErrorCode {

    INVALID_IMAGE_DOMAIN(HttpStatus.BAD_REQUEST, "IMG-001", "유효하지 않은 이미지 도메인입니다."),
    INVALID_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "IMG-002", "허용되지 않은 이미지 확장자입니다."),
    INVALID_IMAGE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "IMG-003", "허용되지 않은 이미지 Content-Type입니다."),
    IMAGE_EXTENSION_CONTENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "IMG-004", "이미지 확장자와 Content-Type이 일치하지 않습니다."),
    INVALID_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "IMG-005", "이미지 용량 정보가 올바르지 않습니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "IMG-006", "이미지 용량은 5MB 이하만 업로드할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ImageErrorCode(HttpStatus status, String code, String message) {
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