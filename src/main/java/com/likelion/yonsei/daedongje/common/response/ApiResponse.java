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
        ApiError error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 본문이 필요 없는 성공 응답 (예: 삭제·로그아웃 등 데이터 없는 200/204).
     *
     * <p>참고: 메서드명이 {@code success()} 가 아닌 {@code successEmpty()} 인 이유는,
     * 자바 record 가 컴포넌트({@code boolean success})와 같은 이름의 메서드를
     * static 이라도 accessor 재선언으로 간주해 컴파일 오류를 내기 때문이다.
     */
    public static ApiResponse<Void> successEmpty() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }

    /**
     * 에러 응답의 body 부분.
     *
     * <p>내부 클래스명을 {@code Error} 가 아닌 {@code ApiError} 로 둔 이유는
     * {@link java.lang.Error} 와 단순 이름 충돌 시 다른 패키지에서 import 혼동을 줄이기 위함.
     */
    public record ApiError(String code, String message) {
    }
}
