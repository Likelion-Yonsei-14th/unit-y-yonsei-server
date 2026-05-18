package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.exception.MapLocationErrorCode;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCreateServiceRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceMyResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final AdminUserRepository adminUserRepository;
    private final MapLocationRepository mapLocationRepository;

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
        if (request == null) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NAME_REQUIRED);
        }

        if (performanceRepository.existsByAdminUser(adminUser)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_EXISTS);
        }

        Performance performance = Performance.create(adminUser, createdByAdmin, request.performanceName());
        MapLocation location = findLocationOrNull(request.locationId());
        performance.updateBasicInfo(
                location,
                null,
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

        return PerformanceMyResponse.from(performance);
    }

    @Transactional
    public void deleteMyPerformance(AdminSessionUser currentAdmin) {
        Performance performance = findMyPerformance(currentAdmin);
        performanceRepository.delete(performance);
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
