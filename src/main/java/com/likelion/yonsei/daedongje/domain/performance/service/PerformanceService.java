package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;

    @Transactional
    public Performance createPerformanceForAdmin(AdminUser adminUser, String performanceName) {
        Performance performance = Performance.create(adminUser, performanceName);
        return performanceRepository.save(performance);
    }
}
