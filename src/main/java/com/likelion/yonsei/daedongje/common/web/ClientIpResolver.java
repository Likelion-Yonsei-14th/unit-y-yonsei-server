package com.likelion.yonsei.daedongje.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청의 클라이언트 IP 주소를 추출한다.
 *
 * <p>서비스가 프록시(예: Railway) 뒤에서 동작할 수 있으므로 {@code X-Forwarded-For} 헤더를
 * 우선 사용하고, 헤더가 없으면 {@link HttpServletRequest#getRemoteAddr()} 로 폴백한다.
 */
public final class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // "client, proxy1, proxy2" 형태이므로 첫 번째 값이 원 클라이언트 IP 다.
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
