package com.likelion.yonsei.daedongje.domain.auth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.CurrentAdminUserResponse;
import com.likelion.yonsei.daedongje.domain.auth.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Auth", description = "어드민 로그인/인증 API")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Value("${server.servlet.session.cookie.name:DDJ_ADMIN_SESSION}")
    private String sessionCookieName;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean sessionCookieSecure;

    @Value("${server.servlet.session.cookie.same-site:lax}")
    private String sessionCookieSameSite;

    @Value("${server.servlet.session.cookie.http-only:true}")
    private boolean sessionCookieHttpOnly;

    @Operation(
            summary = "어드민 로그인",
            description = "생성된 어드민 계정의 로그인 아이디와 비밀번호로 로그인합니다. 로그인 성공 시 Redis 기반 세션이 생성되고 세션 쿠키가 발급됩니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        AdminLoginResponse response = adminAuthService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "현재 로그인한 어드민 조회",
            description = "세션 쿠키를 기반으로 현재 로그인한 어드민 계정 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentAdminUserResponse>> getCurrentAdminUser(
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        CurrentAdminUserResponse response = adminAuthService.getCurrentAdminUser(session);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "어드민 로그아웃",
            description = "현재 요청의 세션을 무효화하고 세션 쿠키를 만료 처리합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) HttpServletRequest httpRequest,
            @Parameter(hidden = true) HttpServletResponse httpResponse
    ) {
        adminAuthService.logout(httpRequest);

        ResponseCookie expiredCookie = ResponseCookie.from(sessionCookieName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(sessionCookieHttpOnly)
                .secure(sessionCookieSecure)
                .sameSite(sessionCookieSameSite)
                .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

        return ResponseEntity.ok(ApiResponse.successEmpty());
    }
}