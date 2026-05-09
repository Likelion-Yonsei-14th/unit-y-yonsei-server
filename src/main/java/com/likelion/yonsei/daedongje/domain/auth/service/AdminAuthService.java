package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.session.AdminSessionConst;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.CurrentAdminUserResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminStatus;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request, HttpServletRequest httpRequest) {
        AdminUser adminUser = adminUserRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_LOGIN_FAILED));

        validatePassword(request.getPassword(), adminUser.getPasswordHash());
        validateActiveStatus(adminUser);

        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(AdminSessionConst.ADMIN_USER_ID, adminUser.getId());
        session.setAttribute(AdminSessionConst.ADMIN_ROLE, adminUser.getRole().name());

        adminUser.updateLastLoginAt();

        return AdminLoginResponse.from(adminUser);
    }

    @Transactional(readOnly = true)
    public CurrentAdminUserResponse getCurrentAdminUser(HttpSession session) {
        if (session == null) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
        }

        Long adminUserId = (Long) session.getAttribute(AdminSessionConst.ADMIN_USER_ID);

        if (adminUserId == null) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED);
        }

        AdminUser adminUser = adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_SESSION));

        validateActiveStatus(adminUser);

        return CurrentAdminUserResponse.from(adminUser);
    }

    private void validatePassword(String rawPassword, String passwordHash) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw new BusinessException(AuthErrorCode.ADMIN_LOGIN_FAILED);
        }
    }

    private void validateActiveStatus(AdminUser adminUser) {
        if (adminUser.getStatus() != AdminStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.INACTIVE_ADMIN_ACCOUNT);
        }
    }
}