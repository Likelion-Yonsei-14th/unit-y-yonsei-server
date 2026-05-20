package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserPasswordChangeRequest;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private AdminUserService adminUserService;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private PerformanceRepository performanceRepository;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    @DisplayName("changeMyPassword rejects when session is missing")
    void changeMyPasswordThrowsWhenSessionMissing() {
        // 세션이 존재하지 않음 == 로그인하지 않음 -> UNAUTHORIZED 에러 던지기
        AdminUserPasswordChangeRequest request = passwordChangeRequest("current", "new");
        when(adminAuthContextService.getCurrentAdminUserEntity(null))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        assertThatThrownBy(() -> adminAuthService.changeMyPassword(request, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.UNAUTHORIZED);

        verify(adminUserService, never()).changeOwnPassword(any(AdminUser.class), any(AdminUserPasswordChangeRequest.class));
    }

    private AdminUserPasswordChangeRequest passwordChangeRequest(String currentPassword, String newPassword) {
        AdminUserPasswordChangeRequest request = new AdminUserPasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", currentPassword);
        ReflectionTestUtils.setField(request, "newPassword", newPassword);
        return request;
    }
}
