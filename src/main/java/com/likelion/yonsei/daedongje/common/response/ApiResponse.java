package com.likelion.yonsei.daedongje.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 REST API 응답을 감싸는 공통 래퍼.
 *
 * <p>형태:
 * <pre>
 *   성공: { "success": true,  "data": {...} }
 *   실패: { "success": false, "error": { "code": "B-001", "message": "..." } }
 * </pre>
 *
 * <p>HTTP status 는 별개로 표준대로 사용한다 (200/400/401/404/500 등).
 * 도메인 분기가 필요한 경우 프론트는 body 의 {@code error.code} 로 처리한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        Error error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new Error(code, message));
    }

    public record Error(String code, String message) {
    }
}
