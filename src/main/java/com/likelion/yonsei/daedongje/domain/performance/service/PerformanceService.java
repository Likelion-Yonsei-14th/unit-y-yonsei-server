package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.exception.MapLocationErrorCode;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCreateServiceRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceMyResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceImageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceSetlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final AdminUserRepository adminUserRepository;
    private final MapLocationRepository mapLocationRepository;
    private final PerformanceImageRepository performanceImageRepository;
    private final PerformanceSetlistRepository performanceSetlistRepository;
    private final NoticeRepository noticeRepository;

    @Transactional
    public Performance createPerformanceForAdmin(AdminUser adminUser, String performanceName) {
        return createPerformanceForAdmin(adminUser, adminUser, createRequest(performanceName));
    }

    @Transactional
    public Performance createPerformanceForAdmin(AdminUser adminUser, PerformanceCreateServiceRequest request) {
        return createPerformanceForAdmin(adminUser, adminUser, request);
    }

    @Transactional
    public Performance createPerformanceForAdmin(AdminUser adminUser, AdminUser createdByAdmin, String performanceName) {
        return createPerformanceForAdmin(adminUser, createdByAdmin, createRequest(performanceName));
    }

    @Transactional
    public Performance createPerformanceForAdmin(
            AdminUser adminUser,
            AdminUser createdByAdmin,
            PerformanceCreateServiceRequest request
    ) {
        if (request == null || !StringUtils.hasText(request.performanceName())) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NAME_REQUIRED);
        }

        if (performanceRepository.existsByAdminUser(adminUser)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_EXISTS);
        }

        String normalizedPerformanceName = request.performanceName().trim();


        Performance performance = Performance.create(
                adminUser,
                createdByAdmin,
                normalizedPerformanceName
        );

        MapLocation location = findLocationOrNull(request.locationId());

        performance.updateBasicInfo(
                location,
                normalizedPerformanceName,
                null,
                request.performanceDate(),
                request.startTime(),
                request.endTime(),
                null,
                null,
                null
        );

        try {
            return performanceRepository.save(performance);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_EXISTS);
        }
    }

    @Transactional
    public Performance createPerformanceForAdmin(
            AdminUser adminUser,
            AdminUser createdByAdmin,
            String performanceName,
            Integer performanceDate,
            Long locationId,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return createPerformanceForAdmin(
                adminUser,
                createdByAdmin,
                new PerformanceCreateServiceRequest(
                        performanceName,
                        performanceDate,
                        locationId,
                        startTime,
                        endTime
                )
        );
    }

    private PerformanceCreateServiceRequest createRequest(String performanceName) {
        return new PerformanceCreateServiceRequest(performanceName, null, null, null, null);
    }

    public PerformanceMyResponse getMyPerformance(AdminSessionUser currentAdmin) {
        Performance performance = findMyPerformance(currentAdmin);
        return PerformanceMyResponse.from(performance);
    }

    @Transactional
    public PerformanceMyResponse updateMyPerformance(AdminSessionUser currentAdmin, PerformanceUpdateRequest request) {
        Performance performance = findMyPerformance(currentAdmin);
        applyBasicInfoUpdate(performance, request);
        return PerformanceMyResponse.from(performance);
    }

    /**
     * 운영진(SUPER/MASTER)이 임의 공연의 기본 정보를 부분 갱신한다.
     * updateMyPerformance 와 갱신 로직은 동일 — "어떤 공연을 찾는가" 만 다르다.
     */
    @Transactional
    public PerformanceMyResponse updatePerformance(Long id, PerformanceUpdateRequest request) {
        Performance performance = findById(id);
        applyBasicInfoUpdate(performance, request);
        return PerformanceMyResponse.from(performance);
    }

    private void applyBasicInfoUpdate(Performance performance, PerformanceUpdateRequest request) {
        MapLocation location = findLocationOrNull(request.locationId());
        performance.updateBasicInfo(
                location,
                request.performanceName(),
                request.performanceDescription(),
                request.performanceDate(),
                request.startTime(),
                request.endTime(),
                request.performanceCategory(),
                request.lineupName(),
                request.performanceStatus(),
                request.hashtag1(),
                request.hashtag2(),
                request.hashtag3(),
                request.youtubeUrl(),
                request.instagramUrl()
        );
    }

    private Performance findById(Long id) {
        return performanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    @Transactional
    public void deleteMyPerformance(AdminSessionUser currentAdmin) {
        Performance performance = findMyPerformance(currentAdmin);
        deletePerformanceWithGuards(performance);
    }

    /**
     * 운영진(SUPER/MASTER)이 임의 공연 ID 로 공연을 삭제한다.
     * deleteMyPerformance 와 자식 가드 로직은 공유 — "어떤 공연을 찾는가" 만 다르다 (BAC-110).
     */
    @Transactional
    public void deletePerformance(Long id) {
        Performance performance = findById(id);
        deletePerformanceWithGuards(performance);
    }

    /**
     * 자식 데이터(이미지/셋리스트/공지) 가 남아 있으면 차단해 실수 삭제 방지 (BAC-110).
     * cheer_messages 는 DB FK 가 ON DELETE CASCADE, live_performance 는 ON DELETE SET NULL 이라 자동 정리되므로 가드 불필요.
     * 검사 후 race 로 자식이 새로 생성된 경우(DataIntegrityViolationException)를 잡아 의미 있는 BusinessException 으로 변환한다.
     */
    private void deletePerformanceWithGuards(Performance performance) {
        Long id = performance.getId();
        verifyNoChildData(id);

        try {
            performanceRepository.delete(performance);
            // FK 검증을 트랜잭션 커밋이 아닌 *여기서* 실행 — 아래 catch 로 잡을 수 있게 함
            performanceRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // race — 검사 시점 이후 자식이 새로 생성됐을 가능성. 다시 확인해 BusinessException 으로 변환.
            verifyNoChildData(id);
            throw e;  // 알려진 자식이 아니면 원본 FK 위반 유지
        }
    }

    private void verifyNoChildData(Long performanceId) {
        if (performanceImageRepository.existsByPerformanceId(performanceId)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_HAS_IMAGES);
        }
        if (performanceSetlistRepository.existsByPerformanceId(performanceId)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_HAS_SETLISTS);
        }
        if (noticeRepository.existsByPerformanceId(performanceId)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_HAS_NOTICES);
        }
    }

    private Performance findMyPerformance(AdminSessionUser currentAdmin) {
        AdminUser adminUser = findPerformanceAdmin(currentAdmin);
        return performanceRepository.findByAdminUser(adminUser)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    private AdminUser findPerformanceAdmin(AdminSessionUser currentAdmin) {
        if (currentAdmin == null) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }
        if (currentAdmin.getRole() != AdminRole.PERFORMER && currentAdmin.getRole() != AdminRole.SUPER) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        return adminUserRepository.findById(currentAdmin.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));
    }

    private MapLocation findLocationOrNull(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return mapLocationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(MapLocationErrorCode.MAP_LOCATION_NOT_FOUND));
    }


}
