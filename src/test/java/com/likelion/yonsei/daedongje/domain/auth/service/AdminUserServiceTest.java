package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserPasswordChangeRequest;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminSessionService adminSessionService;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private BoothService boothService;

    @Mock
    private PerformanceService performanceService;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("Password change stores new hash and invalidates sessions")
    void changeOwnPasswordUpdatesHashAndInvalidatesSessions() {
        // Happy PATH: 비밀번호 변경 성공 -> 새로운 해시 저장 + 세션 무효화
        AdminUser adminUser = adminUser(ADMIN_ID, "current-hash");
        AdminUserPasswordChangeRequest request = passwordChangeRequest("current", "new");
        when(passwordEncoder.matches("current", "current-hash")).thenReturn(true);
        when(passwordEncoder.matches("new", "current-hash")).thenReturn(false);
        when(passwordEncoder.encode("new")).thenReturn("new-hash");

        adminUserService.changeOwnPassword(adminUser, request);

        assertThat(adminUser.getPasswordHash()).isEqualTo("new-hash");
        verify(adminSessionService).invalidateAdminSessions(ADMIN_ID);
    }

    @Test
    @DisplayName("Throws when current password does not match")
    void changeOwnPasswordThrowsWhenCurrentPasswordInvalid() {
        // 입력한 현재 비밀번호 불일치 -> INVALID_CURRENT_PASSWORD 에러
        AdminUser adminUser = adminUser(ADMIN_ID, "current-hash");
        AdminUserPasswordChangeRequest request = passwordChangeRequest("current", "new");
        when(passwordEncoder.matches("current", "current-hash")).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.changeOwnPassword(adminUser, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_CURRENT_PASSWORD);

        verify(adminSessionService, never()).invalidateAdminSessions(anyLong());
    }

    @Test
    @DisplayName("Throws when new password matches current password")
    void changeOwnPasswordThrowsWhenNewPasswordSameAsCurrent() {
        // 새 비밀번호와 현재 비밀번호가 일치하면 PASSWORD_SAME_AS_CURRENT 에러 던지기
        AdminUser adminUser = adminUser(ADMIN_ID, "current-hash");
        AdminUserPasswordChangeRequest request = passwordChangeRequest("current", "new");
        when(passwordEncoder.matches("current", "current-hash")).thenReturn(true);
        when(passwordEncoder.matches("new", "current-hash")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.changeOwnPassword(adminUser, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.PASSWORD_SAME_AS_CURRENT);

        verify(adminSessionService, never()).invalidateAdminSessions(anyLong());
    }

    private AdminUser adminUser(Long id, String passwordHash) {
        AdminUser adminUser = AdminUser.create(
                "admin",
                passwordHash,
                "org",
                AdminRole.BOOTH,
                "representative",
                "01000000000",
                null
        );
        ReflectionTestUtils.setField(adminUser, "id", id);
        return adminUser;
    }

    private AdminUserPasswordChangeRequest passwordChangeRequest(String currentPassword, String newPassword) {
        AdminUserPasswordChangeRequest request = new AdminUserPasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", currentPassword);
        ReflectionTestUtils.setField(request, "newPassword", newPassword);
        return request;
    }
}
