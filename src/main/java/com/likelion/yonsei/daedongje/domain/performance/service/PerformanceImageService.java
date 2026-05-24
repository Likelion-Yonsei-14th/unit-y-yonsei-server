package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceImageCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceImageResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImageType;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceImageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PerformanceImageService {

    private final PerformanceImageRepository performanceImageRepository;
    private final PerformanceRepository performanceRepository;
    private final AdminUserRepository adminUserRepository;

    // 공연당 이미지 최대 개수(PROFILE 1 + DETAIL 다수). 운영 정책상 조정 가능.
    private static final int MAX_IMAGES_PER_PERFORMANCE = 20;

    @Transactional
    public PerformanceImageResponse createMyPerformanceImage(
            AdminSessionUser currentAdmin,
            PerformanceImageCreateRequest request
    ) {
        Performance performance = getMyPerformance(currentAdmin);
        validateImageConstraints(performance.getId(), request);
        PerformanceImage performanceImage = PerformanceImage.create(
                performance,
                request.imageUrl(),
                request.imageOrder(),
                request.imageType()
        );

        return PerformanceImageResponse.from(performanceImageRepository.save(performanceImage));
    }

    // 공연 이미지 등록 규칙: 최대 개수 제한, 대표(PROFILE) 1장, imageOrder 유일.
    private void validateImageConstraints(Long performanceId, PerformanceImageCreateRequest request) {
        if (performanceImageRepository.countByPerformanceId(performanceId) >= MAX_IMAGES_PER_PERFORMANCE) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_IMAGE_LIMIT_EXCEEDED);
        }
        if (request.imageType() == PerformanceImageType.PROFILE
                && performanceImageRepository.existsByPerformanceIdAndImageType(performanceId, PerformanceImageType.PROFILE)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_IMAGE_PROFILE_ALREADY_EXISTS);
        }
        if (performanceImageRepository.existsByPerformanceIdAndImageOrder(performanceId, request.imageOrder())) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_IMAGE_ORDER_DUPLICATED);
        }
    }

    public List<PerformanceImageResponse> getPerformanceImages(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        // 비공개(HIDDEN)·취소(CANCELED) 공연의 이미지는 사용자 페이지에 노출하지 않는다.
        if (performance.getPerformanceStatus() == PerformanceStatus.HIDDEN
                || performance.getPerformanceStatus() == PerformanceStatus.CANCELED) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
        }

        return performanceImageRepository.findAllByPerformanceIdOrderByImageOrderAscIdAsc(performanceId)
                .stream()
                .map(PerformanceImageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteMyPerformanceImage(AdminSessionUser currentAdmin, Long imageId) {
        PerformanceImage performanceImage = performanceImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_IMAGE_NOT_FOUND));
        Performance myPerformance = getMyPerformance(currentAdmin);

        if (!performanceImage.getPerformance().getId().equals(myPerformance.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        performanceImageRepository.delete(performanceImage);
    }

    private Performance getMyPerformance(AdminSessionUser currentAdmin) {
        AdminUser adminUser = adminUserRepository.findById(currentAdmin.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));

        return performanceRepository.findByAdminUser(adminUser)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    public List<PerformanceImageResponse> getAdminPerformanceImages(
            AdminSessionUser currentAdmin,
            Long performanceId
    ) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        validateAdminPerformanceImageAccess(currentAdmin, performance);

        return performanceImageRepository.findAllByPerformanceIdOrderByImageOrderAscIdAsc(performanceId)
                .stream()
                .map(PerformanceImageResponse::from)
                .toList();
    }

    private void validateAdminPerformanceImageAccess(
            AdminSessionUser currentAdmin,
            Performance performance
    ) {
        if (currentAdmin.getRole() == AdminRole.SUPER) {
            return;
        }

        if (currentAdmin.getRole() == AdminRole.PERFORMER
                && performance.getAdminUser().getId().equals(currentAdmin.getId())) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN);
    }
}
