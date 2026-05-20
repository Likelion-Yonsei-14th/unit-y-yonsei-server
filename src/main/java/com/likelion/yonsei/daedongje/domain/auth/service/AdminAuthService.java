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

        LinkedIds linked = resolveLinkedIds(adminUser);
        return AdminLoginResponse.from(adminUser, linked.boothId(), linked.performanceTeamId());
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
        LinkedIds linked = resolveLinkedIds(adminUser);
        return CurrentAdminUserResponse.from(adminUser, linked.boothId(), linked.performanceTeamId());
    }

    /**
     * 어드민 역할별 소유 리소스(부스/공연) ID 묶음.
     * login·getCurrentAdminUser 가 동일한 lookup 로직을 공유하기 위한 헬퍼 반환 타입.
     */
    private record LinkedIds(Long boothId, Long performanceTeamId) {}

    /**
     * 어드민 역할에 따라 소유 리소스 ID 를 조회한다.
     * - BOOTH → boothId 채움, performanceTeamId 는 null
     * - PERFORMER → performanceTeamId 채움, boothId 는 null
     * - 그 외(SUPER/MASTER) 또는 소유 리소스가 없으면 → 둘 다 null
     */
    private LinkedIds resolveLinkedIds(AdminUser adminUser) {
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
        return new LinkedIds(boothId, performanceTeamId);
    }

    @Transactional
    public void changeMyPassword(AdminUserPasswordChangeRequest request, HttpSession session) {
        AdminUser adminUser = adminAuthContextService.getCurrentAdminUserEntity(session);
        adminUserService.changeOwnPassword(adminUser, request);
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