package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LivePerformanceControllerTest {

    private static final String LIVE_URL = "/api/performances/live";

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

    private int adminSequence;

    @BeforeEach
    void setUp() {
        livePerformanceRepository.deleteAll();
        performanceRepository.deleteAll();
        mapLocationRepository.deleteAll();
        adminUserRepository.deleteAll();
        adminSequence = 0;
    }

    @Test
    void getLivePerformance_returns_null_when_no_row_exists() throws Exception {
        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getLivePerformance_returns_null_when_pointer_is_not_pinned() throws Exception {
        livePerformanceRepository.save(LivePerformance.singleton());

        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getLivePerformance_returns_pinned_performance() throws Exception {
        MapLocation location = mapLocationRepository.save(mapLocation("Outdoor Stage"));
        Performance performance = visiblePerformance("Live Stage", location);
        pin(performance);

        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Live Stage"))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Outdoor Stage"));
    }

    @Test
    void getLivePerformance_returns_hidden_performance_as_is() throws Exception {
        // 핀된 공연이 HIDDEN 이어도 운영진이 명시 지정한 값이므로 그대로 반환한다.
        Performance hidden = performanceRepository.save(Performance.create(adminUser(), "Hidden Stage"));
        pin(hidden);

        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(hidden.getId()))
                .andExpect(jsonPath("$.data.performanceStatus").value("HIDDEN"));
    }

    private void pin(Performance performance) {
        LivePerformance livePerformance = LivePerformance.singleton();
        livePerformance.updatePerformance(performance);
        livePerformanceRepository.save(livePerformance);
    }

    private Performance visiblePerformance(String name, MapLocation location) {
        Performance performance = Performance.create(adminUser(), name);
        performance.updateBasicInfo(
                location,
                name,
                name + " description",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.ONGOING
        );
        return performanceRepository.save(performance);
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

    private MapLocation mapLocation(String locationName) {
        return MapLocation.create(
                locationName,
                "A",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                MapLocationType.STAGE,
                1,
                MapDisplayStatus.VISIBLE
        );
    }
}
