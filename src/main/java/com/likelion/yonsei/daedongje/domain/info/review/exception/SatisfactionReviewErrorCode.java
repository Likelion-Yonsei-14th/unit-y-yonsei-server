package com.likelion.yonsei.daedongje.domain.info.review.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SatisfactionReviewErrorCode implements ErrorCode {

    SATISFACTION_REVIEW_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "I-004",
            "만족도 리뷰 제출 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SatisfactionReviewErrorCode(HttpStatus status, String code, String message) {
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
