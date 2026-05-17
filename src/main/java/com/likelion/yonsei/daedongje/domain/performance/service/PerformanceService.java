package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
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

    @Transactional
    public Performance createPerformanceForAdmin(AdminUser adminUser, String performanceName) {
        return createPerformanceForAdmin(adminUser, adminUser, performanceName);
    }

    @Transactional
    public Performance createPerformanceForAdmin(AdminUser adminUser, AdminUser createdByAdmin, String performanceName) {
        Performance performance = Performance.create(adminUser, createdByAdmin, performanceName);

        if (performanceRepository.existsByAdminUser(adminUser)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_EXISTS);
        }

        try {
            return performanceRepository.save(performance);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_EXISTS);
        }
    }
}
