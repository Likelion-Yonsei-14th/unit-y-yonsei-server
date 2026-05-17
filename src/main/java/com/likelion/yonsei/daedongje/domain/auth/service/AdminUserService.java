package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserBulkCreateResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSessionService adminSessionService;

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

    @Transactional
    public AdminUserBulkCreateResponse bulkCreateAdminUsers(MultipartFile file) {
        List<AdminUserBulkCreateResponse.SuccessDetail> successList = new ArrayList<>();
        List<AdminUserBulkCreateResponse.FailDetail> failList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            int rowIndex = 1;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] columns = line.split(",");
                if (columns.length < 5) {
                    rowIndex++;
                    failList.add(new AdminUserBulkCreateResponse.FailDetail("", "", "올바르지 않은 데이터 형식 (컬럼 부족)"));
                    continue; // 빈 줄이거나 컬럼 부족 시 건너뛰기
                }
                String roleStr = columns[0].trim();
                String boothName = columns[1].trim();
                String organization = columns[2].trim();
                String representativeName = columns[3].trim();
                String representativePhone = columns[4].trim();
                AdminRole role = null;
                String reason = null;
                // Role 검증
                try {
                    role = AdminRole.valueOf(roleStr);
                    if (role != AdminRole.BOOTH && role != AdminRole.PERFORMER) {
                        reason = "유효하지 않은 Role(BOOTH or PERFORMER)";
                    }
                } catch (IllegalArgumentException e) {
                    reason = "유효하지 않은 Role(BOOTH or PERFORMER)";
                }
                if (reason != null) {
                    failList.add(new AdminUserBulkCreateResponse.FailDetail(roleStr, boothName, reason));
                    rowIndex++;
                    continue;
                }
                // ID 생성: booth_Name + 순번
                String loginId = boothName + "_" + rowIndex;
                
                // 중복 체크
                if (adminUserRepository.existsByLoginId(loginId)) {
                    failList.add(new AdminUserBulkCreateResponse.FailDetail(roleStr, boothName, "이미 존재하는 로그인 아이디 (" + loginId + ")"));
                    rowIndex++;
                    continue;
                }
                // 랜덤 비밀번호 생성
                String password = generateRandomPassword();
                String passwordHash = passwordEncoder.encode(password);
                // 계정 생성
                AdminUser adminUser = AdminUser.create(
                        loginId,
                        passwordHash,
                        organization,
                        role,
                        representativeName,
                        representativePhone,
                        "일괄 생성 계정" // memo
                );
                try {
                    adminUserRepository.save(adminUser);
                    successList.add(new AdminUserBulkCreateResponse.SuccessDetail(loginId, password, boothName));
                } catch (Exception e) {
                    failList.add(new AdminUserBulkCreateResponse.FailDetail(roleStr, boothName, "계정 생성 중 오류 발생: " + e.getMessage()));
                }
                
                rowIndex++;
            }
            adminUserRepository.flush();
        } catch (Exception e) {
            throw new RuntimeException("CSV 파일 처리 중 오류가 발생했습니다.", e);
        }
        return AdminUserBulkCreateResponse.of(successList.size(), failList.size(), successList, failList);
    }
    private String generateRandomPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "01023456789";
        String specials = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        String allChars = upper + lower + digits + specials;
        SecureRandom random = new SecureRandom();
        int length = 8 + random.nextInt(3); // 8 ~ 10자
        StringBuilder sb = new StringBuilder();
        // 각 종류별 최소 1자 이상 보장
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(specials.charAt(random.nextInt(specials.length())));
        for (int i = 4; i < length; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        // 셔플
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char a = chars[index];
            chars[index] = chars[i];
            chars[i] = a;
        }
        return new String(chars);
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
}