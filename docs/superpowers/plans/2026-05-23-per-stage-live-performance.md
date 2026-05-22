# 무대별 실시간 공연 조회 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 무대별 "지금 진행 중인 공연"을 한 번의 호출로 반환하는 사용자용 읽기 API(`GET /api/performances/live-stages`)를 추가한다. 아티스트 메인 무대는 기존 수동 핀, 동아리 무대는 시간 기반 자동 판정.

**Architecture:** 조회 시점에 계산(read-time computed). `performance_status`는 안 건드리고 스케줄러도 없음. 수동은 기존 `LivePerformance` 싱글톤 핀(`LivePerformanceService`), 자동은 `FestivalDayService`(일차) + 주입된 `Clock`(시각)으로 CLUB 공연을 무대별로 판정. 새 `LiveStageService`가 둘을 조립한다.

**Tech Stack:** Spring Boot, Spring Data JPA, JPA(@EntityGraph/@Query), JUnit5 + MockMvc + Mockito, H2(test) / MySQL(prod), Flyway(스키마 변경 없음).

스펙: `docs/superpowers/specs/2026-05-23-per-stage-live-performance-design.md`

---

## File Structure

**신규**
- `src/main/java/com/likelion/yonsei/daedongje/domain/performance/entity/LiveStageSource.java` — enum `{ MANUAL, AUTO }`.
- `src/main/java/com/likelion/yonsei/daedongje/domain/performance/dto/LiveStageResponse.java` — `{ source, performance }` 응답 DTO (Lombok `@Getter @Builder` 클래스 + 정적 팩토리 `of(...)`).
- `src/main/java/com/likelion/yonsei/daedongje/domain/performance/service/LiveStageService.java` — 무대별 라이브 조립 서비스.
- `src/main/java/com/likelion/yonsei/daedongje/config/ClockConfig.java` — Asia/Seoul `Clock` 빈.
- `src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LiveStageControllerTest.java` — 통합 테스트.

**수정**
- `src/main/java/com/likelion/yonsei/daedongje/domain/performance/repository/PerformanceRepository.java` — 라이브 판정용 CLUB 조회 메서드 추가.
- `src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java` — `/live-stages` 엔드포인트 추가, `/current`에 deprecated 설명.

---

## Task 1: 지원 타입 + Clock 빈 + 리포지토리 쿼리 (스캐폴딩)

행위가 없는 타입/설정/쿼리. 컴파일로만 검증하고, 동작은 Task 2의 테스트가 검증한다.

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/entity/LiveStageSource.java`
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/dto/LiveStageResponse.java`
- Create: `src/main/java/com/likelion/yonsei/daedongje/config/ClockConfig.java`
- Modify: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/repository/PerformanceRepository.java`

- [ ] **Step 1: `LiveStageSource` enum 생성**

`src/main/java/com/likelion/yonsei/daedongje/domain/performance/entity/LiveStageSource.java`:
```java
package com.likelion.yonsei.daedongje.domain.performance.entity;

/** 무대별 라이브 공연의 출처. MANUAL = 운영진 수동 핀(아티스트), AUTO = 시간 기반 자동(동아리). */
public enum LiveStageSource {
    MANUAL,
    AUTO
}
```

- [ ] **Step 2: `LiveStageResponse` DTO 생성**

`src/main/java/com/likelion/yonsei/daedongje/domain/performance/dto/LiveStageResponse.java`:
```java
package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.LiveStageSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "무대별 라이브 공연 응답")
@Getter
@Builder
public class LiveStageResponse {

    @Schema(description = "공연 출처 (MANUAL=수동 핀, AUTO=시간 자동)", example = "AUTO")
    private final LiveStageSource source;

    @Schema(description = "해당 무대에서 현재 진행 중인 공연")
    private final PerformanceCurrentResponse performance;

    public static LiveStageResponse of(LiveStageSource source, PerformanceCurrentResponse performance) {
        return LiveStageResponse.builder()
                .source(source)
                .performance(performance)
                .build();
    }
}
```

- [ ] **Step 3: `ClockConfig` 생성 (Asia/Seoul Clock 빈)**

`src/main/java/com/likelion/yonsei/daedongje/config/ClockConfig.java`:
```java
package com.likelion.yonsei.daedongje.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 시각 의존 로직(라이브 공연 판정 등)이 테스트에서 결정적이도록 Clock 을 빈으로 주입한다.
 * 운영에선 Asia/Seoul 시스템 시계, 테스트에선 고정 Clock 으로 오버라이드한다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
```

- [ ] **Step 4: `PerformanceRepository`에 라이브 판정용 쿼리 추가**

`src/main/java/com/likelion/yonsei/daedongje/domain/performance/repository/PerformanceRepository.java` — import 추가:
```java
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;

import java.util.Collection;
```

`findAllWithLocationByPerformanceStatusNot` 메서드 바로 아래에 추가 (JOIN FETCH 는 inner — location 없는 공연은 무대 귀속 불가라 자동 제외):
```java
    // 무대별 라이브 자동 판정용. 카테고리·일차·제외상태로 1차 필터하고, 시간 윈도우 판정은 서비스에서 한다.
    // location 이 없는 공연은 무대에 귀속할 수 없으므로 inner JOIN FETCH 로 제외한다.
    @Query("SELECT p FROM Performance p JOIN FETCH p.location "
            + "WHERE p.performanceCategory = :category "
            + "AND p.performanceDate = :day "
            + "AND p.performanceStatus NOT IN :excludedStatuses")
    List<Performance> findLiveCandidatesByCategoryAndDay(
            @Param("category") PerformanceCategory category,
            @Param("day") Integer day,
            @Param("excludedStatuses") Collection<PerformanceStatus> excludedStatuses);
```

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL (새 타입·쿼리가 모두 컴파일됨)

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/entity/LiveStageSource.java \
        src/main/java/com/likelion/yonsei/daedongje/domain/performance/dto/LiveStageResponse.java \
        src/main/java/com/likelion/yonsei/daedongje/config/ClockConfig.java \
        src/main/java/com/likelion/yonsei/daedongje/domain/performance/repository/PerformanceRepository.java
git commit -m "feat: 무대별 라이브 공연 조회용 타입·Clock 빈·리포지토리 쿼리 추가"
git push
```

---

## Task 2: `LiveStageService` + 엔드포인트 (TDD, happy-path)

수동 핀 1건 + 동아리 자동 2건을 무대 displayOrder 순으로 반환하는 핵심 경로를 테스트가 먼저 실패하게 만든 뒤 구현한다.

**Files:**
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LiveStageControllerTest.java`
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/service/LiveStageService.java`
- Modify: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java`

- [ ] **Step 1: 실패하는 통합 테스트 작성 (happy-path + 픽스처 헬퍼)**

`src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LiveStageControllerTest.java`:
```java
package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.festival.FestivalDayService;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.service.LivePerformanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LiveStageControllerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private MapLocationRepository mapLocationRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private LivePerformanceRepository livePerformanceRepository;

    @Autowired
    private LivePerformanceService livePerformanceService;

    @MockBean
    private FestivalDayService festivalDayService;

    @MockBean
    private Clock clock;

    private int adminSequence;

    @BeforeEach
    void setUp() {
        livePerformanceRepository.deleteAll();
        performanceRepository.deleteAll();
        mapLocationRepository.deleteAll();
        adminUserRepository.deleteAll();
        adminSequence = 0;

        // 기본: 축제 2일차, 현재 시각 18:30 (Asia/Seoul)
        when(festivalDayService.getCurrentFestivalDay()).thenReturn(2);
        fixClockAt(LocalTime.of(18, 30));
    }

    @Test
    void liveStages_returnsManualPinThenAutoClubStages_orderedByStageDisplayOrder() throws Exception {
        MapLocation mainStage = mapLocationRepository.save(mapLocation("Main Stage", 1));
        MapLocation clubStageA = mapLocationRepository.save(mapLocation("Club Stage A", 2));
        MapLocation clubStageB = mapLocationRepository.save(mapLocation("Club Stage B", 3));

        // 아티스트 핀 (시간 없음, status 무관) — 메인 무대
        Performance artist = performanceRepository.save(performance(
                "Artist Headliner", mainStage, 2, null, null,
                PerformanceCategory.ARTIST, PerformanceStatus.HIDDEN));
        livePerformanceService.updateLivePerformance(artist.getId());

        // 동아리 자동 — 18:30 진행 중 (displayOrder 역순으로 저장해 정렬 검증)
        performanceRepository.save(performance(
                "Club B Now", clubStageB, 2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance(
                "Club A Now", clubStageA, 2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].source").value("MANUAL"))
                .andExpect(jsonPath("$.data[0].performance.performanceName").value("Artist Headliner"))
                .andExpect(jsonPath("$.data[0].performance.locationName").value("Main Stage"))
                .andExpect(jsonPath("$.data[1].source").value("AUTO"))
                .andExpect(jsonPath("$.data[1].performance.performanceName").value("Club A Now"))
                .andExpect(jsonPath("$.data[2].source").value("AUTO"))
                .andExpect(jsonPath("$.data[2].performance.performanceName").value("Club B Now"));
    }

    private void fixClockAt(LocalTime time) {
        Instant instant = LocalDate.of(2026, 5, 27).atTime(time).atZone(SEOUL).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(SEOUL);
    }

    private Performance performance(
            String name,
            MapLocation location,
            Integer performanceDate,
            LocalTime startTime,
            LocalTime endTime,
            PerformanceCategory category,
            PerformanceStatus status
    ) {
        Performance performance = Performance.create(adminUser(), name);
        performance.updateBasicInfo(
                location, name, name + " description", performanceDate,
                startTime, endTime, category, name + " lineup", status);
        return performance;
    }

    private AdminUser adminUser() {
        adminSequence++;
        return adminUserRepository.save(AdminUser.create(
                "performer-" + adminSequence,
                "password-hash",
                "Performance Team " + adminSequence,
                AdminRole.PERFORMER,
                "Representative",
                "010-0000-%04d".formatted(adminSequence),
                null
        ));
    }

    private MapLocation mapLocation(String locationName, int displayOrder) {
        return MapLocation.create(
                locationName, "A",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                MapLocationType.STAGE, displayOrder, MapDisplayStatus.VISIBLE);
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.LiveStageControllerTest"`
Expected: FAIL — `/api/performances/live-stages` 핸들러가 없어 404 (또는 컴파일 단계에서 `LiveStageService` 미존재). 

- [ ] **Step 3: `LiveStageService` 구현**

`src/main/java/com/likelion/yonsei/daedongje/domain/performance/service/LiveStageService.java`:
```java
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

    // start <= now < end (반열린 구간). 시간 미입력·역전(end<=start)은 진행 중 아님.
    private boolean isPlayingNow(Performance performance, LocalTime now) {
        LocalTime start = performance.getStartTime();
        LocalTime end = performance.getEndTime();
        if (start == null || end == null || !end.isAfter(start)) {
            return false;
        }
        return !start.isAfter(now) && end.isAfter(now);
    }
}
```

- [ ] **Step 4: `PerformanceReadController`에 엔드포인트 추가**

`src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java` — import 추가:
```java
import com.likelion.yonsei.daedongje.domain.performance.dto.LiveStageResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.LiveStageService;
```

필드 추가 (기존 `livePerformanceService` 아래):
```java
    private final LiveStageService liveStageService;
```

`getLivePerformance()` 메서드 바로 아래에 추가:
```java
    @Operation(
            summary = "무대별 라이브 공연 조회",
            description = "무대별 현재 진행 중인 공연을 조회합니다. 아티스트 메인 무대는 운영진 수동 지정(MANUAL), "
                    + "동아리 무대는 일차·시작/종료 시간 기반 자동 판정(AUTO)입니다. 진행 중인 무대만 포함됩니다."
    )
    @GetMapping("/live-stages")
    public ApiResponse<List<LiveStageResponse>> getLiveStages() {
        return ApiResponse.success(liveStageService.getLiveStages());
    }
```

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.LiveStageControllerTest"`
Expected: PASS (1 test)

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/service/LiveStageService.java \
        src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LiveStageControllerTest.java
git commit -m "feat: 무대별 라이브 공연 조회 API(/live-stages) 구현"
git push
```

---

## Task 3: 엣지케이스 테스트

`LiveStageControllerTest`에 시나리오를 추가한다. 각 테스트는 Task 2의 픽스처 헬퍼(`performance`, `adminUser`, `mapLocation`, `fixClockAt`)를 사용한다. 완성된 서비스가 이미 통과시키므로 회귀 가드 역할.

**Files:**
- Modify: `src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LiveStageControllerTest.java`

- [ ] **Step 1: 테스트 메서드 추가 (Step 1의 `liveStages_...` 메서드들 아래, 헬퍼들 위에 삽입)**

```java
    @Test
    void liveStages_excludesClubPerformancesOutsideTimeWindow() throws Exception {
        MapLocation stage = mapLocationRepository.save(mapLocation("Club Stage", 1));
        performanceRepository.save(performance(
                "Now Playing", stage, 2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance(
                "Not Yet", mapLocationRepository.save(mapLocation("Club Stage 2", 2)),
                2, LocalTime.of(19, 0), LocalTime.of(20, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance(
                "Already Ended", mapLocationRepository.save(mapLocation("Club Stage 3", 3)),
                2, LocalTime.of(17, 0), LocalTime.of(18, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].performance.performanceName").value("Now Playing"));
    }

    @Test
    void liveStages_includesPerformance_whenNowEqualsStart() throws Exception {
        fixClockAt(LocalTime.of(18, 0));
        MapLocation stage = mapLocationRepository.save(mapLocation("Club Stage", 1));
        performanceRepository.save(performance(
                "Starts Now", stage, 2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].performance.performanceName").value("Starts Now"));
    }

    @Test
    void liveStages_excludesPerformance_whenNowEqualsEnd() throws Exception {
        fixClockAt(LocalTime.of(19, 0));
        MapLocation stage = mapLocationRepository.save(mapLocation("Club Stage", 1));
        performanceRepository.save(performance(
                "Ends Now", stage, 2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void liveStages_excludesHiddenCanceledEndedClubPerformances() throws Exception {
        performanceRepository.save(performance(
                "Hidden Club", mapLocationRepository.save(mapLocation("Stage 1", 1)),
                2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.HIDDEN));
        performanceRepository.save(performance(
                "Canceled Club", mapLocationRepository.save(mapLocation("Stage 2", 2)),
                2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.CANCELED));
        performanceRepository.save(performance(
                "Ended Club", mapLocationRepository.save(mapLocation("Stage 3", 3)),
                2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.ENDED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void liveStages_excludesClubPerformanceWithoutTimes() throws Exception {
        MapLocation stage = mapLocationRepository.save(mapLocation("Club Stage", 1));
        performanceRepository.save(performance(
                "No Times", stage, 2, null, null,
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void liveStages_picksEarliestPerformancePerStage_whenOverlap() throws Exception {
        MapLocation stage = mapLocationRepository.save(mapLocation("Club Stage", 1));
        performanceRepository.save(performance(
                "Later Start", stage, 2, LocalTime.of(18, 15), LocalTime.of(18, 45),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance(
                "Earlier Start", stage, 2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].performance.performanceName").value("Earlier Start"));
    }

    @Test
    void liveStages_returnsEmpty_whenNothingLiveAndNoPin() throws Exception {
        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void liveStages_manualPinWins_whenSameStageAlsoHasPlayingClub() throws Exception {
        MapLocation stage = mapLocationRepository.save(mapLocation("Shared Stage", 1));

        Performance artist = performanceRepository.save(performance(
                "Pinned Artist", stage, 2, null, null,
                PerformanceCategory.ARTIST, PerformanceStatus.HIDDEN));
        livePerformanceService.updateLivePerformance(artist.getId());

        performanceRepository.save(performance(
                "Club On Same Stage", stage, 2, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.CLUB, PerformanceStatus.SCHEDULED));

        mockMvc.perform(get("/api/performances/live-stages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].source").value("MANUAL"))
                .andExpect(jsonPath("$.data[0].performance.performanceName").value("Pinned Artist"));
    }
```

- [ ] **Step 2: 테스트 실행 → 전부 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.LiveStageControllerTest"`
Expected: PASS (9 tests: happy-path 1 + 엣지 8)

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LiveStageControllerTest.java
git commit -m "test: 무대별 라이브 공연 조회 엣지케이스 검증 추가"
git push
```

---

## Task 4: `/current` deprecated 표기 + OpenAPI 노출 검증 + 전체 빌드

**Files:**
- Modify: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java`
- Modify: `src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadControllerTest.java`

- [ ] **Step 1: `/current` 핸들러에 deprecated 설명 추가**

`PerformanceReadController.getCurrentPerformance()`의 `@Operation` 을 아래로 교체:
```java
    @Operation(
            summary = "현재 공연 조회 (deprecated)",
            description = "Deprecated — `/api/performances/live-stages` 사용 권장. status 가 ONGOING 인 공연 중 가장 이른 1건만 반환합니다. "
                    + "진행 중인 공연이 없으면 data 는 null 입니다."
    )
    @Deprecated
    @GetMapping("/current")
    public ApiResponse<PerformanceCurrentResponse> getCurrentPerformance() {
        return ApiResponse.success(performanceReadService.getCurrentPerformance());
    }
```

- [ ] **Step 2: 실패하는 OpenAPI 노출 테스트 작성**

`PerformanceReadControllerTest`의 `openApi_exposes_user_performance_read_apis_without_out_of_scope_posts()` 의 assertion 블록에 한 줄 추가:
```java
        assertThat(paths.has("/api/performances/live-stages")).isTrue();
```
(`assertThat(paths.has("/api/performances/timetable")).isTrue();` 바로 아래에 삽입)

- [ ] **Step 3: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.PerformanceReadControllerTest"`
Expected: PASS — `/live-stages` 가 OpenAPI 문서에 노출됨이 확인됨

- [ ] **Step 4: 전체 테스트 + 빌드**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL — 전체 테스트 통과, 회귀 없음

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadControllerTest.java
git commit -m "docs: /current API deprecated 표기 및 /live-stages OpenAPI 노출 검증"
git push
```

---

## Self-Review (작성자 점검 결과)

**스펙 커버리지**: 읽기시점 계산(Task 2 서비스), 카테고리 축(ARTIST 핀/CLUB 자동 — Task 2), 단일 핀 재사용(LivePerformanceService — Task 2), API 형태/공연 중인 무대만(Task 2), `/current` deprecated·`/live` 유지(Task 4), 상태 제외 HIDDEN/CANCELED/ENDED(Task 1 쿼리 + Task 2 상수, Task 3 검증), 경계 start<=now<end(Task 2 + Task 3), 시간 null·location null·겹침(Task 1 inner join + Task 2 + Task 3), 무대 순서 displayOrder(Task 2 + Task 3), 빈 배열(Task 3), Clock 주입 테스트(Task 2/3), DB 마이그레이션 없음 ✔. 모든 스펙 항목에 대응 태스크 있음.

**플레이스홀더 스캔**: 없음. 모든 코드/명령/기대출력 명시.

**타입 일관성**: `LiveStageSource{MANUAL,AUTO}`, `LiveStageResponse.of(source, performance)`, `PerformanceCurrentResponse.getLocationId()/getLocationName()`(기존 getter), `findLiveCandidatesByCategoryAndDay(category, day, excludedStatuses)`(Task 1 정의 == Task 2 호출), `LivePerformanceService.getLivePerformance()`(기존, `PerformanceCurrentResponse` 반환) — 전 태스크 시그니처 일치 확인.
