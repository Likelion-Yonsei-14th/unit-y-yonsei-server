package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.CurrentAdminUserResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminAuthContextService adminAuthContextService;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    @DisplayName("BOOTH 어드민 로그인 응답에 소유 부스의 boothId 가 채워지고 performanceTeamId 는 null 이다")
    void loginWithBoothRoleReturnsBoothId() {
        AdminUser user = adminUser(10L, AdminRole.BOOTH);
        Booth booth = mock(Booth.class);
        when(booth.getId()).thenReturn(50L);
        when(adminUserRepository.findByLoginId("booth-admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(httpRequest.getSession(false)).thenReturn(null);
        when(httpRequest.getSession(true)).thenReturn(httpSession);
        when(boothRepository.findByAdminId(10L)).thenReturn(Optional.of(booth));

        AdminLoginResponse response = adminAuthService.login(loginRequest("booth-admin"), httpRequest);

        assertThat(response.getBoothId()).isEqualTo(50L);
        assertThat(response.getPerformanceTeamId()).isNull();
    }

    @Test
    @DisplayName("PERFORMER 어드민 로그인 응답에 소유 공연의 performanceTeamId 가 채워지고 boothId 는 null 이다")
    void loginWithPerformerRoleReturnsPerformanceTeamId() {
        AdminUser user = adminUser(20L, AdminRole.PERFORMER);
        Performance perf = mock(Performance.class);
        when(perf.getId()).thenReturn(60L);
        when(adminUserRepository.findByLoginId("performer-admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(httpRequest.getSession(false)).thenReturn(null);
        when(httpRequest.getSession(true)).thenReturn(httpSession);
        when(performanceRepository.findByAdminUser(user)).thenReturn(Optional.of(perf));

        AdminLoginResponse response = adminAuthService.login(loginRequest("performer-admin"), httpRequest);

        assertThat(response.getPerformanceTeamId()).isEqualTo(60L);
        assertThat(response.getBoothId()).isNull();
    }

    @Test
    @DisplayName("SUPER 어드민 로그인 응답에는 boothId·performanceTeamId 가 둘 다 null 이다")
    void loginWithSuperRoleReturnsBothNull() {
        AdminUser user = adminUser(30L, AdminRole.SUPER);
        when(adminUserRepository.findByLoginId("super-admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(httpRequest.getSession(false)).thenReturn(null);
        when(httpRequest.getSession(true)).thenReturn(httpSession);

        AdminLoginResponse response = adminAuthService.login(loginRequest("super-admin"), httpRequest);

        assertThat(response.getBoothId()).isNull();
        assertThat(response.getPerformanceTeamId()).isNull();
    }

    @Test
    @DisplayName("BOOTH 어드민의 /me 응답에도 동일하게 boothId 가 채워진다 (login 과 동일 헬퍼 사용 검증)")
    void getCurrentAdminUserWithBoothRoleReturnsBoothId() {
        AdminUser user = adminUser(10L, AdminRole.BOOTH);
        Booth booth = mock(Booth.class);
        when(booth.getId()).thenReturn(50L);
        when(adminAuthContextService.getCurrentAdminUserEntity(httpSession)).thenReturn(user);
        when(boothRepository.findByAdminId(10L)).thenReturn(Optional.of(booth));

        CurrentAdminUserResponse response = adminAuthService.getCurrentAdminUser(httpSession);

        assertThat(response.getBoothId()).isEqualTo(50L);
        assertThat(response.getPerformanceTeamId()).isNull();
    }

    private AdminUser adminUser(Long id, AdminRole role) {
        AdminUser user = AdminUser.create(
                "login-" + id,
                "passwordHash",
                "조직",
                role,
                "홍길동",
                "010-0000-0000",
                null
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private AdminLoginRequest loginRequest(String loginId) {
        AdminLoginRequest request = new AdminLoginRequest();
        ReflectionTestUtils.setField(request, "loginId", loginId);
        ReflectionTestUtils.setField(request, "password", "password");
        return request;
    }
}
