package com.likelion.yonsei.daedongje.domain.home.service;

import com.likelion.yonsei.daedongje.domain.home.dto.HomeBannerResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceReadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HomeService {

    private final PerformanceReadService performanceReadService;

    public HomeService(PerformanceReadService performanceReadService) {
        this.performanceReadService = performanceReadService;
    }

    public List<HomeBannerResponse> getBanners() {
        return List.of();
    }

    public PerformanceCurrentResponse getCurrentPerformance() {
        return performanceReadService.getCurrentPerformance();
    }
}
