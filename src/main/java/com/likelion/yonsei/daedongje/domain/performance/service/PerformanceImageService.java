package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceImageCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceImageResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImage;
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

    @Transactional
    public PerformanceImageResponse createMyPerformanceImage(
            AdminSessionUser currentAdmin,
            PerformanceImageCreateRequest request
    ) {
        Performance performance = getMyPerformance(currentAdmin);
        PerformanceImage performanceImage = PerformanceImage.create(
                performance,
                request.imageUrl(),
                request.imageOrder(),
                request.imageType()
        );

        return PerformanceImageResponse.from(performanceImageRepository.save(performanceImage));
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
}
