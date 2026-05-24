package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import com.likelion.yonsei.daedongje.domain.monitoring.service.RecentErrorLogService;
import com.likelion.yonsei.daedongje.domain.monitoring.service.SystemHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemStatusController.class)
class SystemStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemHealthService systemHealthService;

    @MockitoBean
    private RecentErrorLogService recentErrorLogService;

    @MockitoBean
    private ActiveAlertStore activeAlertStore;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void authenticateAsSuper() {
        when(adminAuthContextService.getCurrentAdmin(any()))
                .thenReturn(new AdminSessionUser(1L, AdminRole.SUPER, "admin"));
    }

    @Test
    @DisplayName("GET /health 는 시스템 스냅샷을 반환한다")
    void healthReturnsSnapshot() throws Exception {
        SystemHealthResponse.MemoryInfo heap = new SystemHealthResponse.MemoryInfo(500L, 1000L, 0.5);
        SystemHealthResponse.DbPoolInfo pool = new SystemHealthResponse.DbPoolInfo(3, 5, 0, 10);
        when(systemHealthService.snapshot())
                .thenReturn(new SystemHealthResponse("UP", "1.2.3", 3600L, heap, pool, 42, 0.3));

        mockMvc.perform(get("/api/admin/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.heap.usedRatio").value(0.5))
                .andExpect(jsonPath("$.data.dbPool.active").value(3));
    }

    @Test
    @DisplayName("GET /errors 는 최근 ERROR 로그 목록을 반환한다")
    void errorsReturnsRecent() throws Exception {
        when(recentErrorLogService.recent()).thenReturn(List.of(
                new ErrorLogEntry(LocalDateTime.of(2026, 5, 24, 10, 0), "ERROR", "Foo", "boom", null)));

        mockMvc.perform(get("/api/admin/system/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].message").value("boom"))
                .andExpect(jsonPath("$.data[0].level").value("ERROR"));
    }

    @Test
    @DisplayName("GET /alerts 는 현재 활성 알림 목록을 반환한다")
    void alertsReturnsActive() throws Exception {
        when(activeAlertStore.findAllActive()).thenReturn(List.of(
                new ActiveAlertResponse("fp1", "HighErrorRate", "high", "5xx>5%", "2026-05-24T10:00:00Z")));

        mockMvc.perform(get("/api/admin/system/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("HighErrorRate"))
                .andExpect(jsonPath("$.data[0].severity").value("high"));
    }
}
