package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserDetailResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserListResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserPasswordResetRequest;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

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

    // role에 따른 전체 조회 구현, role 유무에 따라 findBy 함수 다르게 써서 응답시간 최적화
    public List<AdminUserListResponse> getAdminUsers(String role) {
        AdminRole filterRole = parseRoleOrNull(role);
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<AdminUser> adminUsers = filterRole == null
            ? adminUserRepository.findAll(sort)
            : adminUserRepository.findAllByRole(filterRole, sort);

        return adminUsers.stream()
            .map(AdminUserListResponse::from)
                .toList();
    }

    public AdminUserDetailResponse getAdminUserDetail(Long id) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));

        return AdminUserDetailResponse.fromDefault(adminUser);
    }


    // 어드민 비밀번호 강제 재설정, 입력받은 비밀번호로 변경
    @Transactional
    public void resetAdminUserPassword(Long id, AdminUserPasswordResetRequest request) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));
        adminUser.changePassword(passwordEncoder.encode(request.getPassword()));    // encoder에 의해 해시로 저장
    }

    // role이 null이거나 빈 문자열인 경우 null을 반환하여 필터링 없이 전체 조회하도록 함
    private AdminRole parseRoleOrNull(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return AdminRole.valueOf(role.toUpperCase(Locale.ROOT));   // 안전장치
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(AuthErrorCode.INVALID_ADMIN_ROLE);
        }
    }

//    InfoComplete 추후 개발
//    private boolean resolveInfoCompleted(AdminUser adminUser) {
//        AdminRole role = adminUser.getRole();
//        if (role == AdminRole.MASTER || role == AdminRole.SUPER) {
//            return true;
//        }
//        return isOrganizationInfoCompleted(adminUser);
//    }
//
//    private boolean isOrganizationInfoCompleted(AdminUser adminUser) {
//        return false;
//    }
}