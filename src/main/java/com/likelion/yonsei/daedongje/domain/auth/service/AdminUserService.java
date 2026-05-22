package com.likelion.yonsei.daedongje.domain.auth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserBulkCreateResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserDetailResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserListResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserPasswordChangeRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserPasswordResetRequest;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;

import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSessionService adminSessionService;
    private final BoothRepository boothRepository;
    private final PerformanceRepository performanceRepository;
    private final BoothService boothService;
    private final PerformanceService performanceService;

    @Transactional
    public AdminUserCreateResponse createAdminUser(AdminUserCreateRequest request, Long currentAdminId) {
        validateDuplicateLoginId(request.getLoginId());

        validateRequiredInfoByRole(request);

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

        AdminUser savedAdminUser = saveAdminUser(adminUser);

        if (request.getRole() == AdminRole.BOOTH) {
            createBoothForNewAdmin(savedAdminUser, request);
        }

        if (request.getRole() == AdminRole.PERFORMER) {
            AdminUser createdByAdmin = findAdminUser(currentAdminId);
            createPerformanceForNewAdmin(savedAdminUser, createdByAdmin, request);
        }

        return AdminUserCreateResponse.from(savedAdminUser);
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
                
                try {
                    String[] columns = parseCsvLine(line);
                    
                    // 5개 미만 컬럼 검사
                    if (columns.length < 5) {
                        failList.add(new AdminUserBulkCreateResponse.FailDetail("", "", "올바르지 않은 데이터 형식 (컬럼 부족)"));
                        rowIndex++;
                        continue;
                    }
                    
                    String roleStr = columns[0].trim();
                    String boothName = columns[1].trim();
                    String organization = columns[2].trim();
                    String representativeName = columns[3].trim();
                    String representativePhone = columns[4].trim();
                    
                    // 빈 줄 검사
                    if (roleStr.isEmpty() && boothName.isEmpty()) {
                        rowIndex++;
                        continue;
                    }
                    
                    // Role 검증
                    AdminRole role;
                    try {
                        role = AdminRole.valueOf(roleStr);
                        if (role != AdminRole.BOOTH && role != AdminRole.PERFORMER) {
                            failList.add(new AdminUserBulkCreateResponse.FailDetail(roleStr, boothName, "유효하지 않은 Role(BOOTH or PERFORMER)"));
                            rowIndex++;
                            continue;
                        }
                    } catch (IllegalArgumentException e) {
                        failList.add(new AdminUserBulkCreateResponse.FailDetail(roleStr, boothName, "유효하지 않은 Role(BOOTH or PERFORMER)"));
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
                            "일괄 생성 계정"
                    );
                    
                    try {
                        adminUserRepository.save(adminUser);
                        successList.add(new AdminUserBulkCreateResponse.SuccessDetail(loginId, password, boothName));
                    } catch (Exception e) {
                        failList.add(new AdminUserBulkCreateResponse.FailDetail(roleStr, boothName, "계정 생성 중 오류 발생: " + e.getMessage()));
                    }
                } catch (Exception e) {
                    failList.add(new AdminUserBulkCreateResponse.FailDetail("", "", "행 처리 중 오류 발생: " + e.getMessage()));
                }
                
                rowIndex++;
            }
            adminUserRepository.flush();
        } catch (Exception e) {
            throw new RuntimeException("CSV 파일 처리 중 오류가 발생했습니다.", e);
        }
        return AdminUserBulkCreateResponse.of(successList.size(), failList.size(), successList, failList);
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // 따옴표 처리: 연속된 따옴표는 이스케이프로 처리
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // 다음 따옴표 스킵
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 따옴표 밖의 콤마는 구분자
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        
        return result.toArray(new String[0]);
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
            throw new BusinessException(AuthErrorCode.LOGIN_ID_DUPLICATED);
        }
    }

    // role에 따른 전체 조회 구현, role 유무에 따라 findBy 함수 다르게 써서 응답시간 최적화
    public List<AdminUserListResponse> getAdminUsers(String role) {
        AdminRole filterRole = parseRoleOrNull(role);
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<AdminUser> adminUsers = filterRole == null
            ? adminUserRepository.findAll(sort)
            : adminUserRepository.findAllByRole(filterRole, sort);

        if (adminUsers.isEmpty()) {
            return List.of();
        }

        List<Long> boothAdminIds = adminUsers.stream()
                .filter(adminUser -> adminUser.getRole() == AdminRole.BOOTH)
                .map(AdminUser::getId)
                .toList();
        List<Long> performerAdminIds = adminUsers.stream()
                .filter(adminUser -> adminUser.getRole() == AdminRole.PERFORMER)
                .map(AdminUser::getId)
                .toList();

        Map<Long, List<Booth>> boothsByAdminId = (boothAdminIds.isEmpty() ? List.<Booth>of() : boothRepository.findAllByAdminIdIn(boothAdminIds)).stream()
                .collect(Collectors.groupingBy(Booth::getAdminId));
        Map<Long, Performance> performanceByAdminId = (performerAdminIds.isEmpty() ? List.<Performance>of() : performanceRepository.findAllByAdminUser_IdIn(performerAdminIds)).stream()
                .collect(Collectors.toMap(
                        performance -> performance.getAdminUser().getId(),
                        performance -> performance,
                        (existing, duplicate) -> existing
                ));

        return adminUsers.stream()

        // Role에 맞는 연관 정보(부스, 공연) 조회하여 응답 DTO에 포함시키기
            .map(adminUser -> {
                List<Booth> linkedBooths = adminUser.getRole() == AdminRole.BOOTH
                        ? boothsByAdminId.getOrDefault(adminUser.getId(), List.of())
                        : null;
                Performance linkedPerformance = adminUser.getRole() == AdminRole.PERFORMER
                        ? performanceByAdminId.get(adminUser.getId())
                        : null;
                boolean infoCompleted = resolveInfoCompleted(adminUser, linkedBooths);
                return AdminUserListResponse.from(adminUser, infoCompleted, linkedBooths, linkedPerformance);
            })
                .toList();
    }

    public AdminUserDetailResponse getAdminUserDetail(Long id) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));

        List<Booth> linkedBooths = adminUser.getRole() == AdminRole.BOOTH
                ? boothRepository.findAllByAdminIdIn(List.of(adminUser.getId()))
                : null;
        Performance linkedPerformance = adminUser.getRole() == AdminRole.PERFORMER
                ? resolveLinkedPerformance(performanceRepository.findAllByAdminUser_IdIn(List.of(adminUser.getId())))
                : null;

        boolean infoCompleted = resolveInfoCompleted(adminUser, linkedBooths);
        return AdminUserDetailResponse.from(adminUser, infoCompleted, linkedBooths, linkedPerformance);
    }

    private void changePasswordAndInvalidate(AdminUser adminUser, String rawPassword) {
        adminUser.changePassword(passwordEncoder.encode(rawPassword));    // encoder에 의해 해시로 저장
        invalidateSessionsAfterCommit(adminUser.getId());  // 비밀번호 변경 커밋 후 세션 무효화
    }

    // 커밋 이후(비밀번호 변경 확정) 세션 무효화 흐름으로 교체
    private void invalidateSessionsAfterCommit(Long adminUserId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            adminSessionService.invalidateAdminSessions(adminUserId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                adminSessionService.invalidateAdminSessions(adminUserId);
            }
        });
    }

    // 어드민 비밀번호 강제 재설정, 입력받은 비밀번호로 변경
    @Transactional
    public void resetAdminUserPassword(Long id, AdminUserPasswordResetRequest request) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));
        if (adminUser.getRole() == AdminRole.SUPER) {
            throw new BusinessException(AuthErrorCode.SUPER_PASSWORD_RESET_FORBIDDEN);
        }
        changePasswordAndInvalidate(adminUser, request.getPassword());  // 새로 작성된 메서드로 변경
    }

    // 세션 정보로 부터 로그인한 계정의 비밀번호 변경
    @Transactional
    public void changeOwnPassword(AdminUser adminUser, AdminUserPasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), adminUser.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (passwordEncoder.matches(request.getNewPassword(), adminUser.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_SAME_AS_CURRENT);
        }
        changePasswordAndInvalidate(adminUser, request.getNewPassword());
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

    private boolean resolveInfoCompleted(AdminUser adminUser, List<Booth> linkedBooths) {
        AdminRole role = adminUser.getRole();
        if (role == AdminRole.MASTER || role == AdminRole.SUPER) {
            return true;
        }
        if (role == AdminRole.BOOTH) {
            if (linkedBooths == null || linkedBooths.isEmpty()) {
                return false;
            }
            return linkedBooths.stream().anyMatch(Booth::isProfileComplete);
        }
        return false;
    }

    private Performance resolveLinkedPerformance(List<Performance> performances) {
        if (performances == null || performances.isEmpty()) {
            return null;
        }
        return performances.get(0);
    }

// BOOTH 어드민 생성 시 부스 기본 정보 함께 생성

    private void createBoothForNewAdmin(AdminUser boothAdmin, AdminUserCreateRequest request) {
        BoothCreateRequest boothRequest = new BoothCreateRequest(
                boothAdmin.getId(),                    // adminId
                request.getBoothName(),                // name
                request.getOrganization(),             // organization
                request.getBoothLocationMemo(),        // description
                request.getBoothOperatingDate(),       // date
                null,                                  // openTime
                null,                                  // closeTime
                request.getBoothSector(),              // sector
                null,                                  // location
                BoothStatus.PREPARING,                 // status
                false,                                 // isFood
                null,                                  // instagram
                false,                                 // isReservable
                null,                                  // account
                null,                                  // locationId
                null,                                  // representativeMenus
                false,                                 // isFoodTruck
                null                                   // notice
        );

        boothService.create(boothRequest);
    }

    // PERFORMER 어드민 생성 시 공연 기본 정보 함께 생성
    private void createPerformanceForNewAdmin(
            AdminUser performerAdmin,
            AdminUser createdByAdmin,
            AdminUserCreateRequest request
    ) {
        performanceService.createPerformanceForAdmin(
                performerAdmin,
                createdByAdmin,
                request.getPerformanceName(),
                request.getPerformanceDate(),
                request.getPerformanceLocationId(),
                request.getPerformanceStartTime(),
                request.getPerformanceEndTime()
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

        // 3. PERFORMER 어드민의 경우, 소유 공연이 있는지 확인
        if (adminUser.getRole() == AdminRole.PERFORMER) {
            boolean hasOwnedPerformances = performanceRepository.existsByAdminUser(adminUser);
            if (hasOwnedPerformances) {
                throw new BusinessException(AuthErrorCode.ADMIN_HAS_OWNED_PERFORMANCES);
            }
        }
    }

    private void validateRequiredInfoByRole(AdminUserCreateRequest request) {
        if (request.getRole() == AdminRole.BOOTH) {
            if (!StringUtils.hasText(request.getBoothName())) {
                throw new BusinessException(AuthErrorCode.BOOTH_INFO_REQUIRED);
            }

            validateBoothOperatingDate(request.getBoothOperatingDate());
        }

        if (request.getRole() == AdminRole.PERFORMER) {
            validatePerformerRequest(request);
        }
    }

    private AdminUser saveAdminUser(AdminUser adminUser) {
        try {
            return adminUserRepository.saveAndFlush(adminUser);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(AuthErrorCode.LOGIN_ID_DUPLICATED);
        }
    }

    private void validateBoothOperatingDate(Integer boothOperatingDate) {
        if (boothOperatingDate == null) {
            return;
        }

        if (boothOperatingDate < 2 || boothOperatingDate > 4) {
            throw new BusinessException(AuthErrorCode.INVALID_BOOTH_OPERATING_DATE);
        }
    }

    private AdminUser findAdminUser(Long id) {
        return adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));
    }

    private void validatePerformerRequest(AdminUserCreateRequest request) {
        if (!StringUtils.hasText(request.getPerformanceName())) {
            throw new BusinessException(AuthErrorCode.PERFORMER_INFO_REQUIRED);
        }

        if (request.getPerformanceName().length() > 100) {
            throw new BusinessException(AuthErrorCode.INVALID_PERFORMANCE_NAME_LENGTH);
        }

        Integer performanceDate = request.getPerformanceDate();
        if (performanceDate != null && (performanceDate < 2 || performanceDate > 4)) {
            throw new BusinessException(AuthErrorCode.INVALID_PERFORMANCE_DATE);
        }

        LocalTime startTime = request.getPerformanceStartTime();
        LocalTime endTime = request.getPerformanceEndTime();

        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new BusinessException(AuthErrorCode.INVALID_PERFORMANCE_TIME);
        }
    }
}