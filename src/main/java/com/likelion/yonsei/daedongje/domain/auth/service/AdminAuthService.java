package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.session.AdminSessionConst;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminLoginResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserPasswordChangeRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.CurrentAdminUserResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminStatus;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuthContextService adminAuthContextService;
    private final AdminUserService adminUserService;
    private final BoothRepository boothRepository;
    private final PerformanceRepository performanceRepository;

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
        session.setAttribute(
            FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
            String.valueOf(adminUser.getId())
        );

        adminUser.updateLastLoginAt();

        return AdminLoginResponse.from(adminUser);
    }

    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional(readOnly = true)
    public CurrentAdminUserResponse getCurrentAdminUser(HttpSession session) {
        AdminUser adminUser = adminAuthContextService.getCurrentAdminUserEntity(session);

        Long boothId = null;
        Long performanceTeamId = null;
        if (adminUser.getRole() == AdminRole.BOOTH) {
            boothId = boothRepository.findByAdminId(adminUser.getId())
                    .map(Booth::getId)
                    .orElse(null);
        } else if (adminUser.getRole() == AdminRole.PERFORMER) {
            performanceTeamId = performanceRepository.findByAdminUser(adminUser)
                    .map(Performance::getId)
                    .orElse(null);
        }

        return CurrentAdminUserResponse.from(adminUser, boothId, performanceTeamId);
    }

    @Transactional
    public void changeMyPassword(AdminUserPasswordChangeRequest request, HttpSession session) {
        AdminUser adminUser = adminAuthContextService.getCurrentAdminUserEntity(session);
        adminUserService.changeOwnPassword(adminUser, request);
        invalidateSessionAfterCommit(session);
    }

    private void invalidateSessionAfterCommit(HttpSession session) {
        if (session == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            session.invalidate();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                session.invalidate();
            }
        });
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