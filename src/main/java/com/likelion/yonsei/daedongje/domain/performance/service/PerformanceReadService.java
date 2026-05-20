package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceDetailResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceListResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceTimetableResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceReadService {

    private static final Comparator<Performance> PERFORMANCE_ORDER = Comparator
            .comparing(Performance::getPerformanceDate, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(Performance::getStartTime, Comparator.nullsLast(LocalTime::compareTo))
            .thenComparing(Performance::getId, Comparator.nullsLast(Long::compareTo));

    private final PerformanceRepository performanceRepository;

    public List<PerformanceListResponse> getPerformances() {
        return findPublicPerformances().stream()
                .map(PerformanceListResponse::from)
                .toList();
    }

    public PerformanceDetailResponse getPerformanceDetail(Long id) {
        Performance performance = performanceRepository.findByIdAndPerformanceStatusNot(id, PerformanceStatus.HIDDEN)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
        return PerformanceDetailResponse.from(performance);
    }

    /**
     * 현재 진행 중(ONGOING) 인 공연 중 가장 이른 1건을 반환한다.
     * 진행 중 공연이 없으면 null 을 반환 — "지금 공연 없음" 은 정상 상태로 표현 (LivePerformanceService.getLivePerformance 와 동일 패턴).
     */
    public PerformanceCurrentResponse getCurrentPerformance() {
        return performanceRepository.findAllByPerformanceStatus(PerformanceStatus.ONGOING).stream()
                .sorted(PERFORMANCE_ORDER)
                .findFirst()
                .map(PerformanceCurrentResponse::from)
                .orElse(null);
    }

    public List<PerformanceTimetableResponse> getPerformanceTimetable() {
        return findPublicPerformances().stream()
                .map(PerformanceTimetableResponse::from)
                .toList();
    }

    private List<Performance> findPublicPerformances() {
        return performanceRepository.findAllWithLocationByPerformanceStatusNot(PerformanceStatus.HIDDEN).stream()
                .sorted(PERFORMANCE_ORDER)
                .toList();
    }
}
