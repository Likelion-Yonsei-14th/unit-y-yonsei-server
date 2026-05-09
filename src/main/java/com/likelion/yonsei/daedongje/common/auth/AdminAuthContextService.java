package com.likelion.yonsei.daedongje.common.auth;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.session.AdminSessionConst;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminStatus;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthContextService {

    private final AdminUserRepository adminUserRepository;

    public AdminSessionUser getCurrentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        AdminUser adminUser = getCurrentAdminUser(session);
        return AdminSessionUser.from(adminUser);
    }

    public AdminUser getCurrentAdminUser(HttpSession session) {
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

        return adminUser;
    }

    private void validateActiveStatus(AdminUser adminUser) {
        if (adminUser.getStatus() != AdminStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.INACTIVE_ADMIN_ACCOUNT);
        }
    }
}