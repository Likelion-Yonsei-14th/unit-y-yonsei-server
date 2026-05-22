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
