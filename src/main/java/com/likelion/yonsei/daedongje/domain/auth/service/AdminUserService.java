package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserDetailResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserListResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private static final String INFO_NOT_APPLICABLE = "-";
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminUserCreateResponse createAdminUser(AdminUserCreateRequest request) {
        validateDuplicateLoginId(request.getLoginId());

        String passwordHash = passwordEncoder.encode(request.getPassword());

        AdminUser adminUser = AdminUser.create(
                request.getLoginId(),
                passwordHash,
                request.getOrganization(),
                request.getRole(),
                request.getRepresentativeName(),
                request.getRepresentativePhone(),
                request.getMemo()
        );

        try {
            AdminUser savedAdminUser = adminUserRepository.saveAndFlush(adminUser);
            return AdminUserCreateResponse.from(savedAdminUser);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(AuthErrorCode.LOGIN_ID_DUPLICATED);
        }

    }

    private void validateDuplicateLoginId(String loginId) {
        if (adminUserRepository.existsByLoginId(loginId)) {
            throw new BusinessException(AuthErrorCode.LOGIN_ID_DUPLICATED
            );
        }
    }

    public List<AdminUserListResponse> getAdminUsers(String role) {
        AdminRole filterRole = parseRoleOrNull(role);
        return adminUserRepository.findAll().stream()
                .filter(adminUser -> filterRole == null || adminUser.getRole() == filterRole)
                .map(adminUser -> AdminUserListResponse.from(adminUser, resolveInfoCompleted(adminUser)))
                .toList();
    }

    public AdminUserDetailResponse getAdminUserDetail(Long id) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));

        String infoCompleted = resolveInfoCompleted(adminUser);
        return AdminUserDetailResponse.fromDefault(adminUser, infoCompleted);
    }

    // role이 null이거나 빈 문자열인 경우 null을 반환하여 필터링 없이 전체 조회하도록 함
    private AdminRole parseRoleOrNull(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return AdminRole.valueOf(role.toUpperCase());   // 안전장치
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(AuthErrorCode.INVALID_ADMIN_ROLE);
        }
    }

    private String resolveInfoCompleted(AdminUser adminUser) {
        return INFO_NOT_APPLICABLE;
    }
}