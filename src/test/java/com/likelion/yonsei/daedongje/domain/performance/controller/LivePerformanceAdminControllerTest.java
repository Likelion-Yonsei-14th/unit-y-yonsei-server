package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LivePerformanceAdminControllerTest {

    private static final String LIVE_URL = "/api/admin/performances/live";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private LivePerformanceRepository livePerformanceRepository;

    @MockBean
    private AdminAuthContextService adminAuthContextService;

    private int adminSequence;

    @BeforeEach
    void setUp() {
        livePerformanceRepository.deleteAll();
        performanceRepository.deleteAll();
        adminUserRepository.deleteAll();
        Mockito.reset(adminAuthContextService);
        adminSequence = 0;

        AdminUser superAdmin = adminUserRepository.save(adminUser("super", AdminRole.SUPER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(superAdmin));
    }

    @Test
    void putLive_pins_performance_and_persists() throws Exception {
        Performance performance = performanceRepository.save(performerPerformance("Live Stage"));

        String body = """
                { "performanceId": %d }
                """.formatted(performance.getId());

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Live Stage"));

        LivePerformance saved = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID).orElseThrow();
        assertThat(saved.getPerformance().getId()).isEqualTo(performance.getId());
    }

    @Test
    void putLive_replaces_existing_pinned_performance() throws Exception {
        Performance first = performanceRepository.save(performerPerformance("First Stage"));
        Performance second = performanceRepository.save(performerPerformance("Second Stage"));
        LivePerformance pointer = LivePerformance.singleton();
        pointer.updatePerformance(first);
        livePerformanceRepository.save(pointer);

        String body = """
                { "performanceId": %d }
                """.formatted(second.getId());

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(second.getId()));

        LivePerformance saved = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID).orElseThrow();
        assertThat(saved.getPerformance().getId()).isEqualTo(second.getId());
    }

    @Test
    void putLive_clears_pointer_when_performance_id_is_null() throws Exception {
        Performance performance = performanceRepository.save(performerPerformance("Live Stage"));
        LivePerformance pointer = LivePerformance.singleton();
        pointer.updatePerformance(performance);
        livePerformanceRepository.save(pointer);

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"performanceId\": null }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        LivePerformance saved = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID).orElseThrow();
        assertThat(saved.getPerformance()).isNull();
    }

    @Test
    void putLive_returns_not_found_when_performance_does_not_exist() throws Exception {
        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"performanceId\": 999999 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void putLive_is_rejected_for_non_super_admin() throws Exception {
        AdminUser performerAdmin = adminUserRepository.save(adminUser("performer-admin", AdminRole.PERFORMER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(performerAdmin));

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"performanceId\": null }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    private Performance performerPerformance(String name) {
        AdminUser performer = adminUserRepository.save(adminUser("performer", AdminRole.PERFORMER));
        return Performance.create(performer, name);
    }

    private AdminUser adminUser(String loginIdPrefix, AdminRole role) {
        adminSequence++;
        return AdminUser.create(
                loginIdPrefix + "-" + adminSequence,
                "password-hash",
                "Team " + adminSequence,
                role,
                "Representative",
                "010-0000-%04d".formatted(adminSequence),
                null
        );
    }
}
