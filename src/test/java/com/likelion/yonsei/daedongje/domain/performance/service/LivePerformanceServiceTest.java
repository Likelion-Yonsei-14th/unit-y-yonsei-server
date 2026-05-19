package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivePerformanceServiceTest {

    @Mock
    private LivePerformanceRepository livePerformanceRepository;

    @Mock
    private PerformanceRepository performanceRepository;

    @InjectMocks
    private LivePerformanceService livePerformanceService;

    @Test
    void getLivePerformance_returns_null_when_row_is_absent() {
        when(livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.empty());

        assertThat(livePerformanceService.getLivePerformance()).isNull();
    }

    @Test
    void getLivePerformance_returns_null_when_no_performance_is_pinned() {
        when(livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.of(LivePerformance.singleton()));

        assertThat(livePerformanceService.getLivePerformance()).isNull();
    }

    @Test
    void getLivePerformance_returns_pinned_performance() {
        LivePerformance livePerformance = LivePerformance.singleton();
        livePerformance.updatePerformance(performance(12L));
        when(livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.of(livePerformance));

        PerformanceCurrentResponse response = livePerformanceService.getLivePerformance();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(12L);
    }

    @Test
    void updateLivePerformance_pins_performance_and_creates_row_when_absent() {
        Performance performance = performance(12L);
        when(performanceRepository.findById(12L)).thenReturn(Optional.of(performance));
        when(livePerformanceRepository.findById(LivePerformance.SINGLETON_ID)).thenReturn(Optional.empty());
        when(livePerformanceRepository.save(any(LivePerformance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerformanceCurrentResponse response = livePerformanceService.updateLivePerformance(12L);

        ArgumentCaptor<LivePerformance> captor = ArgumentCaptor.forClass(LivePerformance.class);
        verify(livePerformanceRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(LivePerformance.SINGLETON_ID);
        assertThat(captor.getValue().getPerformance()).isEqualTo(performance);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(12L);
    }

    @Test
    void updateLivePerformance_clears_pointer_when_performance_id_is_null() {
        when(livePerformanceRepository.findById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.of(LivePerformance.singleton()));
        when(livePerformanceRepository.save(any(LivePerformance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerformanceCurrentResponse response = livePerformanceService.updateLivePerformance(null);

        ArgumentCaptor<LivePerformance> captor = ArgumentCaptor.forClass(LivePerformance.class);
        verify(livePerformanceRepository).save(captor.capture());
        assertThat(captor.getValue().getPerformance()).isNull();
        assertThat(response).isNull();
    }

    @Test
    void updateLivePerformance_throws_when_performance_does_not_exist() {
        when(performanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> livePerformanceService.updateLivePerformance(99L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    private Performance performance(Long id) {
        Performance performance = BeanUtils.instantiateClass(Performance.class);
        ReflectionTestUtils.setField(performance, "id", id);
        ReflectionTestUtils.setField(performance, "performanceName", "Main Stage");
        return performance;
    }
}
