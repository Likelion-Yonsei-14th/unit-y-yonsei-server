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

import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceService;
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
    private final AdminSessionService adminSessionService;
    private final BoothRepository boothRepository;
    private final BoothService boothService;
    private final PerformanceService performanceService;

    @Transactional
    public AdminUserCreateResponse createAdminUser(AdminUserCreateRequest request) {
        validateDuplicateLoginId(request.getLoginId());

        // BOOTH role의 경우 부스 이름 필수
        if (request.getRole() == AdminRole.BOOTH) {
            if (request.getBoothName() == null || request.getBoothName().isBlank()) {
                throw new BusinessException(AuthErrorCode.BOOTH_INFO_REQUIRED);
            }
        }

        // PERFORMER role의 경우 공연 이름 필수
        if (request.getRole() == AdminRole.PERFORMER) {
            if (request.getPerformanceName() == null || request.getPerformanceName().isBlank()) {
                throw new BusinessException(AuthErrorCode.PERFORMER_INFO_REQUIRED);
            }
        }


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

            // BOOTH 역할이면 부스 생성
            if (request.getRole() == AdminRole.BOOTH) {
                createBoothForNewAdmin(savedAdminUser, request);
            }

            // PERFORMER 역할이면 공연 생성
            if (request.getRole() == AdminRole.PERFORMER) {
                createPerformanceForNewAdmin(savedAdminUser, request);
            }

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
        if (adminUser.getRole() == AdminRole.SUPER) {
            throw new BusinessException(AuthErrorCode.SUPER_PASSWORD_RESET_FORBIDDEN);
        }
        adminUser.changePassword(passwordEncoder.encode(request.getPassword()));    // encoder에 의해 해시로 저장
        adminSessionService.invalidateAdminSessions(adminUser.getId());  // 비밀번호 변경 시 기존 세션 무효화(삭제)하여 강제 로그아웃
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

// BOOTH 어드민 생성 시 부스 기본 정보 함께 생성

    private void createBoothForNewAdmin(AdminUser boothAdmin, AdminUserCreateRequest request) {
    Integer boothDate = parseFirstBoothDate(request.getBoothOperatingDates());

    BoothCreateRequest boothRequest = new BoothCreateRequest(
            boothAdmin.getId(),                    // adminId
            request.getBoothName(),                // name (필수)
            request.getOrganization(),             // organization
            request.getBoothLocationMemo(),        // description (자리 메모 저장)
            boothDate,                             // date
            null,                                  // openTime
            null,                                  // closeTime
            request.getBoothSector(),              // sector
            null,                                  // location
            BoothStatus.PREPARING,                 // status
            false,                                 // isFood
            null,                                  // instagram
            false,                                 // isReservable
            null,                                  // account
            null                                   // locationId
    );

    boothService.create(boothRequest);
}

    // 운영 날짜 파싱
    private Integer parseFirstBoothDate(String boothOperatingDates) {
        if (boothOperatingDates == null || boothOperatingDates.isBlank()) {
            return null;
        }

        try {
            String[] dates = boothOperatingDates.split(",");
            if (dates.length > 0) {
                return Integer.parseInt(dates[0].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    // PERFORMER 어드민 생성 시 공연 기본 정보 함께 생성
    private void createPerformanceForNewAdmin(AdminUser performerAdmin, AdminUserCreateRequest request) {
        performanceService.createPerformanceForAdmin(
                performerAdmin,
                request.getPerformanceName()
        );
    }

    @Transactional
    public void deleteAdminUser(Long id) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));

        validateDeletableAdminUser(adminUser);

        adminUserRepository.delete(adminUser);
    }

    private void validateDeletableAdminUser(AdminUser adminUser) {
        // 1. SUPER 계정 삭제 방지
        if (adminUser.getRole() == AdminRole.SUPER) {
            throw new BusinessException(AuthErrorCode.SUPER_ADMIN_DELETE_NOT_ALLOWED);
        }

        // 2. BOOTH 어드민의 경우, 소유 부스가 있는지 확인
        if (adminUser.getRole() == AdminRole.BOOTH) {
            boolean hasOwnedBooths = boothRepository.existsByAdminId(adminUser.getId());
            if (hasOwnedBooths) {
                throw new BusinessException(AuthErrorCode.ADMIN_HAS_OWNED_BOOTHS);
            }
        }
    }
}