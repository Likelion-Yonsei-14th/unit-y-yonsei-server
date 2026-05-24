package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AlertWebhookController.class,
        properties = "monitoring.webhook.secret=")
class AlertWebhookControllerBlankSecretTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActiveAlertStore activeAlertStore;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("시크릿이 미설정(공백)이면 올바른 형식의 요청도 모두 401로 거부한다(안전 기본값)")
    void blankSecretDeniesEverything() throws Exception {
        String body = "{\"status\":\"firing\",\"alerts\":[]}";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer anything")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MON-001"));

        verifyNoInteractions(activeAlertStore);
    }
}
