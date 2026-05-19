package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진이 수동 지정하는 '라이브 공연' 포인터의 조회·갱신을 담당한다.
 *
 * <p>라이브 포인터는 {@code performanceStatus} 와 직교한다 — 지정/해제는 공연 상태를 바꾸지 않는다.
 * 핀된 공연이 {@code HIDDEN} 상태여도 {@link #getLivePerformance()} 는 그대로 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LivePerformanceService {

    private final LivePerformanceRepository livePerformanceRepository;
    private final PerformanceRepository performanceRepository;

    /** 현재 라이브 공연. 미지정(행 없음 또는 performance 가 null)이면 {@code null} 을 반환한다. */
    public PerformanceCurrentResponse getLivePerformance() {
        return livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID)
                .map(LivePerformance::getPerformance)
                .map(PerformanceCurrentResponse::from)
                .orElse(null);
    }

    /**
     * 라이브 공연을 지정/교체하거나({@code performanceId} 지정) 해제한다({@code performanceId == null}).
     * 시드 행이 없으면 싱글톤 행을 새로 만든다.
     */
    @Transactional
    public PerformanceCurrentResponse updateLivePerformance(Long performanceId) {
        Performance performance = resolvePerformance(performanceId);

        LivePerformance livePerformance = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID)
                .orElseGet(LivePerformance::singleton);
        livePerformance.updatePerformance(performance);
        livePerformanceRepository.save(livePerformance);

        return performance == null ? null : PerformanceCurrentResponse.from(performance);
    }

    private Performance resolvePerformance(Long performanceId) {
        if (performanceId == null) {
            return null;
        }
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }
}
