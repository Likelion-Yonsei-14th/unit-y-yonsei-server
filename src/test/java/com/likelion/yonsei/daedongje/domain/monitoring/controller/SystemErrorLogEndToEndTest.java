package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그 캡처 파이프라인 종단 검증.
 * 실제 {@code log.error} → Logback {@code RECENT_ERROR} 어펜더 → 링버퍼 →
 * {@code RecentErrorLogService} → {@code SystemStatusController} → HTTP 응답까지
 * 한 흐름으로 확인한다(H2 사용, Docker/외부 의존 불필요).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SystemErrorLogEndToEndTest {

    private static final Logger log = LoggerFactory.getLogger(SystemErrorLogEndToEndTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @BeforeEach
    void setUp() {
        when(adminAuthContextService.getCurrentAdmin(any()))
                .thenReturn(new AdminSessionUser(1L, AdminRole.SUPER, "admin"));
        RecentErrorLogBuffer.getInstance().clear();
    }

    @Test
    @DisplayName("실제 ERROR 로그가 /api/admin/system/errors 응답까지 종단 전달된다")
    void errorLogFlowsThroughToEndpoint() throws Exception {
        log.error("e2e-capture-boom", new IllegalStateException("e2e-cause"));

        mockMvc.perform(get("/api/admin/system/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.message == 'e2e-capture-boom')]").exists())
                .andExpect(jsonPath("$.data[?(@.message == 'e2e-capture-boom')].level").value("ERROR"))
                .andExpect(jsonPath("$.data[?(@.message == 'e2e-capture-boom')].throwable").exists());
    }

    @Test
    @DisplayName("INFO 로그는 /errors 응답에 포함되지 않는다(ERROR만 캡처)")
    void infoLogDoesNotAppearInEndpoint() throws Exception {
        log.info("e2e-info-skip");

        mockMvc.perform(get("/api/admin/system/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.message == 'e2e-info-skip')]").doesNotExist());
    }
}
