package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.festival.FestivalDayService;
import com.likelion.yonsei.daedongje.domain.performance.dto.LiveStageResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.LiveStageSource;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 무대별 "현재 진행 중인 공연"을 조립한다.
 * 아티스트 메인 무대는 운영진 수동 핀(LivePerformanceService), 동아리 무대는 시간 기반 자동 판정.
 * status 를 바꾸지 않고 조회 시점에 계산한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveStageService {

    // 자동 판정에서 제외할 상태: 미공개·취소·운영진이 명시 종료한 공연.
    private static final Set<PerformanceStatus> EXCLUDED_STATUSES =
            Set.of(PerformanceStatus.HIDDEN, PerformanceStatus.CANCELED, PerformanceStatus.ENDED);

    private final LivePerformanceService livePerformanceService;
    private final PerformanceRepository performanceRepository;
    private final FestivalDayService festivalDayService;
    private final Clock clock;

    public List<LiveStageResponse> getLiveStages() {
        List<LiveStageResponse> result = new ArrayList<>();
        Set<Long> usedLocationIds = new HashSet<>();

        // 1) 수동 핀(아티스트 메인 무대) — status 무관 노출, 핀이 있으면 항상 먼저.
        PerformanceCurrentResponse manual = livePerformanceService.getLivePerformance();
        if (manual != null) {
            result.add(LiveStageResponse.of(LiveStageSource.MANUAL, manual));
            if (manual.getLocationId() != null) {
                usedLocationIds.add(manual.getLocationId());
            }
        }

        // 2) 동아리 무대 자동 — 일차+시간 윈도우로 판정, 무대별 1건.
        int today = festivalDayService.getCurrentFestivalDay();
        LocalTime now = LocalTime.now(clock);

        List<Performance> playingByStage = performanceRepository
                .findLiveCandidatesByCategoryAndDay(PerformanceCategory.CLUB, today, EXCLUDED_STATUSES)
                .stream()
                .filter(performance -> isPlayingNow(performance, now))
                .collect(Collectors.groupingBy(performance -> performance.getLocation().getId()))
                .values().stream()
                .map(group -> group.stream()
                        .min(Comparator.comparing(Performance::getStartTime)
                                .thenComparing(Performance::getId))
                        .orElseThrow())
                .sorted(Comparator
                        .comparing((Performance performance) -> performance.getLocation().getDisplayOrder(),
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(performance -> performance.getLocation().getId()))
                .toList();

        for (Performance performance : playingByStage) {
            if (usedLocationIds.contains(performance.getLocation().getId())) {
                continue; // 수동 핀이 이미 차지한 무대 → 수동 우선
            }
            result.add(LiveStageResponse.of(LiveStageSource.AUTO, PerformanceCurrentResponse.from(performance)));
        }

        return result;
    }

    // start <= now < end (반열린 구간). 시간 미입력은 진행 중 아님(정상).
    // end<=start 는 엔티티 검증을 우회한 데이터 오류이므로 제외하고 경고 로그를 남긴다.
    private boolean isPlayingNow(Performance performance, LocalTime now) {
        LocalTime start = performance.getStartTime();
        LocalTime end = performance.getEndTime();
        if (start == null || end == null) {
            return false;
        }
        if (!end.isAfter(start)) {
            log.warn("CLUB 공연 id={} 시간 역전(start={}, end={}) — 라이브 판정에서 제외", performance.getId(), start, end);
            return false;
        }
        return !start.isAfter(now) && end.isAfter(now);
    }
}
