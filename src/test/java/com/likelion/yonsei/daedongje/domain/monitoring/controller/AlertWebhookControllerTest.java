package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AlertWebhookController.class,
        properties = "monitoring.webhook.secret=test-secret")
class AlertWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActiveAlertStore activeAlertStore;

    // AdminRoleInterceptor가 WebMvcTest 컨텍스트에서 요구하는 빈 (이 컨트롤러는 admin 세션을 쓰지 않음)
    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("올바른 시크릿 + firing 알림이면 활성 알림으로 저장한다")
    void firingStoresAlert() throws Exception {
        String body = """
                {"status":"firing","alerts":[
                  {"status":"firing","labels":{"alertname":"HighErrorRate","severity":"high"},
                   "annotations":{"summary":"5xx>5%"},"startsAt":"2026-05-24T10:00:00Z",
                   "fingerprint":"fp1"}]}""";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<ActiveAlertResponse> captor = ArgumentCaptor.forClass(ActiveAlertResponse.class);
        verify(activeAlertStore).upsert(captor.capture());
        assertThat(captor.getValue().fingerprint()).isEqualTo("fp1");
        assertThat(captor.getValue().name()).isEqualTo("HighErrorRate");
        assertThat(captor.getValue().severity()).isEqualTo("high");
        assertThat(captor.getValue().summary()).isEqualTo("5xx>5%");
    }

    @Test
    @DisplayName("resolved 알림이면 활성 알림에서 제거한다")
    void resolvedRemovesAlert() throws Exception {
        String body = """
                {"status":"resolved","alerts":[
                  {"status":"resolved","fingerprint":"fp1"}]}""";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(activeAlertStore).remove("fp1");
    }

    @Test
    @DisplayName("시크릿이 틀리면 401(MON-001)을 반환하고 저장소를 건드리지 않는다")
    void wrongSecretReturns401() throws Exception {
        String body = "{\"status\":\"firing\",\"alerts\":[]}";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer WRONG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MON-001"));

        verifyNoInteractions(activeAlertStore);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
    void missingHeaderReturns401() throws Exception {
        String body = "{\"status\":\"firing\",\"alerts\":[]}";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MON-001"));

        verifyNoInteractions(activeAlertStore);
    }

    @Test
    @DisplayName("fingerprint이 없는 firing 알림은 저장하지 않고 무시한다")
    void firingWithoutFingerprintIsIgnored() throws Exception {
        String body = """
                {"status":"firing","alerts":[
                  {"status":"firing","labels":{"alertname":"X","severity":"high"},
                   "annotations":{"summary":"no fingerprint"},"startsAt":"2026-05-24T10:00:00Z"}]}""";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(activeAlertStore, never()).upsert(any());
    }
}
