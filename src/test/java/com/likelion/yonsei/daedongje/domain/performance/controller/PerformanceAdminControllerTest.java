package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PerformanceAdminControllerTest {

    private static final String MY_PERFORMANCE_URL = "/api/admin/performances/me";

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

    @MockBean
    private AdminAuthContextService adminAuthContextService;

    private AdminUser performerAdmin;

    @BeforeEach
    void setUp() {
        performanceRepository.deleteAll();
        mapLocationRepository.deleteAll();
        adminUserRepository.deleteAll();
        Mockito.reset(adminAuthContextService);

        performerAdmin = adminUserRepository.save(adminUser("performer", AdminRole.PERFORMER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(performerAdmin));
    }

    @Test
    void getMyPerformance_returns_connected_performance() throws Exception {
        MapLocation location = mapLocationRepository.save(mapLocation("Outdoor Stage"));
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                location,
                "Main Stage",
                "Main stage description",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        mockMvc.perform(get(MY_PERFORMANCE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(performance.getId()))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Outdoor Stage"))
                .andExpect(jsonPath("$.data.performanceName").value("Main Stage"))
                .andExpect(jsonPath("$.data.performanceDescription").value("Main stage description"))
                .andExpect(jsonPath("$.data.performanceDate").value(1))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.lineupName").value("Lineup A"))
                .andExpect(jsonPath("$.data.performanceStatus").value("SCHEDULED"));
    }

    @Test
    void getMyPerformance_fails_when_no_performance_is_connected() throws Exception {
        mockMvc.perform(get(MY_PERFORMANCE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void updateMyPerformance_updates_only_non_null_fields() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                "Before description",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        String requestBody = """
                {
                  "performanceName": "Updated Stage",
                  "performanceDate": 2
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceName").value("Updated Stage"))
                .andExpect(jsonPath("$.data.performanceDescription").value("Before description"))
                .andExpect(jsonPath("$.data.performanceDate").value(2))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.lineupName").value("Lineup A"))
                .andExpect(jsonPath("$.data.performanceStatus").value("SCHEDULED"));
    }

    @Test
    void updateMyPerformance_updates_allowed_fields_and_persists_to_db() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                "Before description",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        String requestBody = """
            {
              "performanceName": "Updated Stage",
              "performanceDescription": "Updated description",
              "performanceDate": 2,
              "startTime": "18:30:00",
              "endTime": "20:30:00",
              "lineupName": "Updated Lineup"
            }
            """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceName").value("Updated Stage"))
                .andExpect(jsonPath("$.data.performanceDescription").value("Updated description"))
                .andExpect(jsonPath("$.data.performanceDate").value(2))
                .andExpect(jsonPath("$.data.startTime").value("18:30:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:30:00"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.lineupName").value("Updated Lineup"))
                .andExpect(jsonPath("$.data.performanceStatus").value("SCHEDULED"));

        Performance updated = performanceRepository.findById(performance.getId()).orElseThrow();
        assertThat(updated.getPerformanceName()).isEqualTo("Updated Stage");
        assertThat(updated.getPerformanceDescription()).isEqualTo("Updated description");
        assertThat(updated.getPerformanceDate()).isEqualTo(2);
        assertThat(updated.getStartTime()).isEqualTo(LocalTime.of(18, 30));
        assertThat(updated.getEndTime()).isEqualTo(LocalTime.of(20, 30));
        assertThat(updated.getLineupName()).isEqualTo("Updated Lineup");

        assertThat(updated.getPerformanceCategory()).isEqualTo(PerformanceCategory.ARTIST);
        assertThat(updated.getPerformanceStatus()).isEqualTo(PerformanceStatus.SCHEDULED);
    }

    @Test
    void updateMyPerformance_updates_name_only_and_preserves_existing_values() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                "Before description",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.CLUB,
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        String requestBody = """
                {
                  "performanceName": "Name Only Updated"
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceName").value("Name Only Updated"))
                .andExpect(jsonPath("$.data.performanceDescription").value("Before description"))
                .andExpect(jsonPath("$.data.performanceDate").value(1))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceCategory").value("CLUB"))
                .andExpect(jsonPath("$.data.lineupName").value("Lineup A"))
                .andExpect(jsonPath("$.data.performanceStatus").value("SCHEDULED"));
    }

    @Test
    void updateMyPerformance_preserves_existing_values_when_fields_are_null() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                "Before description",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        String requestBody = objectMapper.writeValueAsString(new EmptyRequest());

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceName").value("Main Stage"))
                .andExpect(jsonPath("$.data.performanceDescription").value("Before description"))
                .andExpect(jsonPath("$.data.performanceDate").value(1))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.lineupName").value("Lineup A"))
                .andExpect(jsonPath("$.data.performanceStatus").value("SCHEDULED"));
    }

    @Test
    void updateMyPerformance_updates_hashtags_and_sns_links() throws Exception {
        Performance performance = performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "hashtag1": "JPOP",
                  "hashtag2": "인디",
                  "hashtag3": "밴드",
                  "youtubeUrl": "https://www.youtube.com/@yonsei",
                  "instagramUrl": "https://www.instagram.com/yonsei"
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hashtag1").value("JPOP"))
                .andExpect(jsonPath("$.data.hashtag2").value("인디"))
                .andExpect(jsonPath("$.data.hashtag3").value("밴드"))
                .andExpect(jsonPath("$.data.youtubeUrl").value("https://www.youtube.com/@yonsei"))
                .andExpect(jsonPath("$.data.instagramUrl").value("https://www.instagram.com/yonsei"));

        Performance updated = performanceRepository.findById(performance.getId()).orElseThrow();
        assertThat(updated.getHashtag1()).isEqualTo("JPOP");
        assertThat(updated.getHashtag2()).isEqualTo("인디");
        assertThat(updated.getHashtag3()).isEqualTo("밴드");
        assertThat(updated.getYoutubeUrl()).isEqualTo("https://www.youtube.com/@yonsei");
        assertThat(updated.getInstagramUrl()).isEqualTo("https://www.instagram.com/yonsei");
    }

    @Test
    void updateMyPerformance_accepts_hashtags_up_to_six_characters() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "hashtag1": "123456"
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hashtag1").value("123456"));
    }

    @Test
    void updateMyPerformance_rejects_hashtag_over_six_characters() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "hashtag1": "1234567"
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }

    @Test
    void updateMyPerformance_rejects_blank_hashtag() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "hashtag1": "   "
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }

    @Test
    void updateMyPerformance_preserves_hashtags_and_sns_links_when_fields_are_null() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "JPOP",
                "인디",
                "밴드",
                "https://www.youtube.com/@yonsei",
                "https://www.instagram.com/yonsei"
        );
        performanceRepository.save(performance);

        String requestBody = objectMapper.writeValueAsString(new EmptyRequest());

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hashtag1").value("JPOP"))
                .andExpect(jsonPath("$.data.hashtag2").value("인디"))
                .andExpect(jsonPath("$.data.hashtag3").value("밴드"))
                .andExpect(jsonPath("$.data.youtubeUrl").value("https://www.youtube.com/@yonsei"))
                .andExpect(jsonPath("$.data.instagramUrl").value("https://www.instagram.com/yonsei"));
    }

    @Test
    void updateMyPerformance_allows_null_sns_links() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "youtubeUrl": null,
                  "instagramUrl": null
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.youtubeUrl").doesNotExist())
                .andExpect(jsonPath("$.data.instagramUrl").doesNotExist());
    }

    @Test
    void getMyPerformance_includes_hashtags_and_sns_links() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "JPOP",
                "인디",
                "밴드",
                "https://www.youtube.com/@yonsei",
                "https://www.instagram.com/yonsei"
        );
        performanceRepository.save(performance);

        mockMvc.perform(get(MY_PERFORMANCE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hashtag1").value("JPOP"))
                .andExpect(jsonPath("$.data.hashtag2").value("인디"))
                .andExpect(jsonPath("$.data.hashtag3").value("밴드"))
                .andExpect(jsonPath("$.data.youtubeUrl").value("https://www.youtube.com/@yonsei"))
                .andExpect(jsonPath("$.data.instagramUrl").value("https://www.instagram.com/yonsei"));
    }

    @Test
    void updateMyPerformance_rejects_performance_category_update() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                null,
                null,
                null,
                null,
                PerformanceCategory.ARTIST,
                null,
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        String requestBody = """
            {
              "performanceCategory": "CLUB"
            }
            """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-013"));

        Performance updated = performanceRepository.findById(performance.getId()).orElseThrow();
        assertThat(updated.getPerformanceCategory()).isEqualTo(PerformanceCategory.ARTIST);
        assertThat(updated.getPerformanceStatus()).isEqualTo(PerformanceStatus.SCHEDULED);
    }

    @Test
    void updateMyPerformance_rejects_performance_status_update() throws Exception {
        Performance performance = Performance.create(performerAdmin, "Main Stage");
        performance.updateBasicInfo(
                null,
                "Main Stage",
                null,
                null,
                null,
                null,
                PerformanceCategory.ARTIST,
                null,
                PerformanceStatus.SCHEDULED
        );
        performanceRepository.save(performance);

        String requestBody = """
            {
              "performanceStatus": "ONGOING"
            }
            """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-013"));

        Performance updated = performanceRepository.findById(performance.getId()).orElseThrow();
        assertThat(updated.getPerformanceCategory()).isEqualTo(PerformanceCategory.ARTIST);
        assertThat(updated.getPerformanceStatus()).isEqualTo(PerformanceStatus.SCHEDULED);
    }

    @Test
    void updateMyPerformance_rejects_unknown_performance_category() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "performanceCategory": "BAND"
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }

    @Test
    void updateMyPerformance_rejects_blank_performance_name() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "performanceName": " "
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("P-001"));
    }

    @Test
    void updateMyPerformance_updates_location_when_valid_location_id_is_provided() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));
        MapLocation location = mapLocationRepository.save(mapLocation("Baekyang-ro"));

        String requestBody = """
                {
                  "locationId": %d
                }
                """.formatted(location.getId());

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Baekyang-ro"));
    }

    @Test
    void updateMyPerformance_fails_when_location_id_does_not_exist() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        String requestBody = """
                {
                  "locationId": 999999
                }
                """;

        mockMvc.perform(patch(MY_PERFORMANCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("M-001"));
    }

    @Test
    void deleteMyPerformance_deletes_only_current_admin_performance() throws Exception {
        Performance myPerformance = performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));
        AdminUser otherAdmin = adminUserRepository.save(adminUser("other-performer", AdminRole.PERFORMER));
        Performance otherPerformance = performanceRepository.save(Performance.create(otherAdmin, "Other Stage"));

        mockMvc.perform(delete(MY_PERFORMANCE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(performanceRepository.findById(myPerformance.getId())).isEmpty();
        assertThat(performanceRepository.findById(otherPerformance.getId())).isPresent();
    }

    @Test
    void getMyPerformance_fails_after_delete() throws Exception {
        performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        mockMvc.perform(delete(MY_PERFORMANCE_URL))
                .andExpect(status().isOk());

        mockMvc.perform(get(MY_PERFORMANCE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void master_admin_access_is_rejected() throws Exception {
        AdminUser masterAdmin = adminUserRepository.save(adminUser("master", AdminRole.MASTER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(masterAdmin));

        mockMvc.perform(get(MY_PERFORMANCE_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void booth_admin_access_is_rejected() throws Exception {
        AdminUser boothAdmin = adminUserRepository.save(adminUser("booth", AdminRole.BOOTH));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(boothAdmin));

        mockMvc.perform(get(MY_PERFORMANCE_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void super_admin_access_returns_not_found_when_no_performance_is_connected() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("super", AdminRole.SUPER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(superAdmin));

        mockMvc.perform(get(MY_PERFORMANCE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void updatePerformance_with_super_admin_returns_updated_performance() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("super-update", AdminRole.SUPER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(superAdmin));

        Performance performance = performanceRepository.save(Performance.create(performerAdmin, "Original Stage"));

        String requestBody = """
            {
              "performanceName": "Updated by Super",
              "performanceDate": 2,
              "performanceCategory": "CLUB",
              "performanceStatus": "SCHEDULED"
            }
            """;

        mockMvc.perform(patch("/api/admin/performances/" + performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.performanceName").value("Updated by Super"))
                .andExpect(jsonPath("$.data.performanceDate").value(2))
                .andExpect(jsonPath("$.data.performanceCategory").value("CLUB"))
                .andExpect(jsonPath("$.data.performanceStatus").value("SCHEDULED"));
    }

    @Test
    void updatePerformance_with_master_admin_succeeds() throws Exception {
        AdminUser masterAdmin = adminUserRepository.save(adminUser("master-update", AdminRole.MASTER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(masterAdmin));

        Performance performance = performanceRepository.save(Performance.create(performerAdmin, "Original"));

        mockMvc.perform(patch("/api/admin/performances/" + performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performanceName\":\"Updated by Master\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceName").value("Updated by Master"));
    }

    @Test
    void updatePerformance_with_performer_admin_returns_forbidden() throws Exception {
        // BeforeEach 에서 performerAdmin 으로 이미 mock 됨 — 클래스 기본 {PERFORMER, SUPER} 이지만
        // 메서드 @RequireAdminRole({SUPER, MASTER}) 가 덮어쓰므로 PERFORMER 는 차단돼야 한다
        Performance performance = performanceRepository.save(Performance.create(performerAdmin, "Original"));

        mockMvc.perform(patch("/api/admin/performances/" + performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performanceName\":\"Should be blocked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePerformance_returns_not_found_when_id_does_not_exist() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("super-missing", AdminRole.SUPER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(superAdmin));

        mockMvc.perform(patch("/api/admin/performances/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performanceName\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void deletePerformance_with_super_admin_succeeds() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("super-del", AdminRole.SUPER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(superAdmin));

        Performance performance = performanceRepository.save(Performance.create(performerAdmin, "Stage to delete"));
        Long performanceId = performance.getId();

        mockMvc.perform(delete("/api/admin/performances/" + performanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(performanceRepository.findById(performanceId)).isEmpty();
    }

    @Test
    void deletePerformance_with_master_admin_succeeds() throws Exception {
        AdminUser masterAdmin = adminUserRepository.save(adminUser("master-del", AdminRole.MASTER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(masterAdmin));

        Performance performance = performanceRepository.save(Performance.create(performerAdmin, "Stage to delete"));

        mockMvc.perform(delete("/api/admin/performances/" + performance.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void deletePerformance_with_performer_admin_returns_forbidden() throws Exception {
        // BeforeEach 의 performerAdmin 으로 이미 mock 됨 — 메서드 권한 {SUPER, MASTER} 가 클래스 {PERFORMER, SUPER} 를 덮어쓰므로 PERFORMER 차단
        Performance performance = performanceRepository.save(Performance.create(performerAdmin, "Stage"));

        mockMvc.perform(delete("/api/admin/performances/" + performance.getId()))
                .andExpect(status().isForbidden());

        assertThat(performanceRepository.findById(performance.getId())).isPresent();
    }

    @Test
    void deletePerformance_returns_not_found_when_id_does_not_exist() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("super-del-missing", AdminRole.SUPER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(superAdmin));

        mockMvc.perform(delete("/api/admin/performances/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void openApi_exposes_performance_admin_apis_only_for_current_admin_resource() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode myPerformancePath = apiDocs.path("paths").path(MY_PERFORMANCE_URL);

        assertThat(myPerformancePath.isMissingNode()).isFalse();
        assertThat(myPerformancePath.has("get")).isTrue();
        assertThat(myPerformancePath.has("patch")).isTrue();
        assertThat(myPerformancePath.has("delete")).isTrue();
        assertThat(myPerformancePath.has("post")).isFalse();

        assertThat(myPerformancePath.path("get").path("tags").toString()).contains("공연 어드민");
        assertThat(myPerformancePath.path("patch").path("tags").toString()).contains("공연 어드민");
        assertThat(myPerformancePath.path("delete").path("tags").toString()).contains("공연 어드민");

        // 운영진(SUPER·MASTER) 의 공연 정보 수정·삭제 — PATCH·DELETE /{id} 노출 (BAC-106 + BAC-110)
        JsonNode adminUpdatePath = apiDocs.path("paths").path("/api/admin/performances/{id}");
        assertThat(adminUpdatePath.isMissingNode()).isFalse();
        assertThat(adminUpdatePath.has("patch")).isTrue();
        assertThat(adminUpdatePath.has("delete")).isTrue();
        assertThat(adminUpdatePath.has("get")).isFalse();
        assertThat(adminUpdatePath.has("post")).isFalse();
        assertThat(adminUpdatePath.path("patch").path("tags").toString()).contains("공연 어드민");
        assertThat(adminUpdatePath.path("delete").path("tags").toString()).contains("공연 어드민");
    }

    private AdminUser adminUser(String loginId, AdminRole role) {
        return AdminUser.create(
                loginId,
                "password-hash",
                "Performance Team",
                role,
                "Representative",
                "010-0000-0000",
                null
        );
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

    private record EmptyRequest() {
    }
}
