package com.likelion.yonsei.daedongje.domain.home.service;

import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceReadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private PerformanceReadService performanceReadService;

    @InjectMocks
    private HomeService homeService;

    @Test
    @DisplayName("현재 진행 중인 공연 조회는 공연 조회 서비스를 재사용한다")
    void getCurrentPerformanceDelegatesToPerformanceReadService() {
        PerformanceCurrentResponse response = PerformanceCurrentResponse.builder()
                .id(1L)
                .performanceName("연세 밴드부 YB")
                .build();
        when(performanceReadService.getCurrentPerformance()).thenReturn(response);

        PerformanceCurrentResponse result = homeService.getCurrentPerformance();

        assertThat(result).isSameAs(response);
        verify(performanceReadService).getCurrentPerformance();
    }
}
