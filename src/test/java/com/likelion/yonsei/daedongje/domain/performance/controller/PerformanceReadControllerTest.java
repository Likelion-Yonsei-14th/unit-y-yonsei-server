package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PerformanceReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private MapLocationRepository mapLocationRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    private int adminSequence;

    @BeforeEach
    void setUp() {
        performanceRepository.deleteAll();
        mapLocationRepository.deleteAll();
        adminUserRepository.deleteAll();
        adminSequence = 0;
    }

    @Test
    void getCurrentPerformance_returns_ongoing_performance() throws Exception {
        MapLocation location = mapLocationRepository.save(mapLocation("Outdoor Stage"));
        Performance performance = performance(
                "Current Stage",
                location,
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.ONGOING
        );
        performanceRepository.save(performance);

        mockMvc.perform(get("/api/performances/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Current Stage"))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Outdoor Stage"));
    }

    @Test
    void getCurrentPerformance_returns_earliest_ongoing_performance() throws Exception {
        performanceRepository.save(performance(
                "Late Current",
                null,
                2,
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.CLUB,
                "Lineup B",
                PerformanceStatus.ONGOING
        ));
        performanceRepository.save(performance(
                "Early Current",
                null,
                1,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.ONGOING
        ));

        mockMvc.perform(get("/api/performances/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceName").value("Early Current"));
    }

    @Test
    void getCurrentPerformance_returns_not_found_when_no_ongoing_performance_exists() throws Exception {
        performanceRepository.save(performance(
                "Scheduled Stage",
                null,
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.SCHEDULED
        ));

        mockMvc.perform(get("/api/performances/current"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getPerformanceDetail_returns_basic_detail_without_images_or_setlists() throws Exception {
        MapLocation location = mapLocationRepository.save(mapLocation("Main Stage"));
        Performance performance = performance(
                "Detail Stage",
                location,
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.CLUB,
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        mockMvc.perform(get("/api/performances/{id}", performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Detail Stage"))
                .andExpect(jsonPath("$.data.performanceDescription").value("Detail Stage description"))
                .andExpect(jsonPath("$.data.performanceDate").value(1))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceCategory").value("CLUB"))
                .andExpect(jsonPath("$.data.lineupName").value("Lineup A"))
                .andExpect(jsonPath("$.data.performanceStatus").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Main Stage"))
                .andExpect(jsonPath("$.data.performanceImages").doesNotExist())
                .andExpect(jsonPath("$.data.setlists").doesNotExist());
    }

    @Test
    void getPerformanceDetail_fails_for_non_existing_id() throws Exception {
        mockMvc.perform(get("/api/performances/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getPerformanceDetail_fails_for_hidden_performance() throws Exception {
        Performance hidden = performanceRepository.save(Performance.create(adminUser(), "Hidden Stage"));

        mockMvc.perform(get("/api/performances/{id}", hidden.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getPerformanceTimetable_returns_sorted_results() throws Exception {
        performanceRepository.save(performance("Day2 Early", null, 2, LocalTime.of(12, 0), LocalTime.of(13, 0),
                PerformanceCategory.CLUB, "Lineup C", PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance("Day1 Late", null, 1, LocalTime.of(20, 0), LocalTime.of(21, 0),
                PerformanceCategory.ARTIST, "Lineup B", PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance("Day1 Early", null, 1, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.ARTIST, "Lineup A", PerformanceStatus.SCHEDULED));
        performanceRepository.save(Performance.create(adminUser(), "Hidden Stage"));

        mockMvc.perform(get("/api/performances/timetable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].performanceName").value("Day1 Early"))
                .andExpect(jsonPath("$.data[1].performanceName").value("Day1 Late"))
                .andExpect(jsonPath("$.data[2].performanceName").value("Day2 Early"));
    }

    @Test
    void getPerformances_returns_sorted_list() throws Exception {
        performanceRepository.save(performance("Day2 Early", null, 2, LocalTime.of(12, 0), LocalTime.of(13, 0),
                PerformanceCategory.CLUB, "Lineup C", PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance("Day1 Late", null, 1, LocalTime.of(20, 0), LocalTime.of(21, 0),
                PerformanceCategory.ARTIST, "Lineup B", PerformanceStatus.SCHEDULED));
        performanceRepository.save(performance("Day1 Early", null, 1, LocalTime.of(18, 0), LocalTime.of(19, 0),
                PerformanceCategory.ARTIST, "Lineup A", PerformanceStatus.SCHEDULED));
        performanceRepository.save(Performance.create(adminUser(), "Hidden Stage"));

        mockMvc.perform(get("/api/performances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].performanceName").value("Day1 Early"))
                .andExpect(jsonPath("$.data[1].performanceName").value("Day1 Late"))
                .andExpect(jsonPath("$.data[2].performanceName").value("Day2 Early"));
    }

    @Test
    void readApis_work_without_admin_authentication() throws Exception {
        mockMvc.perform(get("/api/performances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void openApi_exposes_user_performance_read_apis_without_out_of_scope_posts() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = apiDocs.path("paths");

        assertThat(apiDocs.path("tags").toString()).contains("공연");
        assertThat(paths.has("/api/performances")).isTrue();
        assertThat(paths.has("/api/performances/{id}")).isTrue();
        assertThat(paths.has("/api/performances/current")).isTrue();
        assertThat(paths.has("/api/performances/timetable")).isTrue();
        assertThat(paths.path("/api/performances").has("post")).isFalse();
        assertThat(paths.path("/api/admin/performances").has("post")).isFalse();
        assertThat(paths.path("/api/performances/{id}").has("post")).isFalse();
    }

    private Performance performance(
            String performanceName,
            MapLocation location,
            Integer performanceDate,
            LocalTime startTime,
            LocalTime endTime,
            PerformanceCategory performanceCategory,
            String lineupName,
            PerformanceStatus performanceStatus
    ) {
        Performance performance = Performance.create(adminUser(), performanceName);
        performance.updateBasicInfo(
                location,
                performanceName,
                performanceName + " description",
                performanceDate,
                startTime,
                endTime,
                performanceCategory,
                lineupName,
                performanceStatus
        );
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
