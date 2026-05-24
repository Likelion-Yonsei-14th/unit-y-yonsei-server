package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.GrafanaWebhookRequest;
import com.likelion.yonsei.daedongje.domain.monitoring.exception.MonitoringErrorCode;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Grafana Cloud 알림 웹훅 수신(내부용). 관리자 세션이 아니라 공유 시크릿으로 보호한다.
 * firing→활성 알림 저장, resolved→제거.
 */
@Tag(name = "모니터링 웹훅", description = "Grafana Cloud 알림 웹훅 수신 (내부용)")
@RestController
@RequestMapping("/internal/monitoring")
public class AlertWebhookController {

    private static final String FIRING = "firing";
    private static final String RESOLVED = "resolved";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ActiveAlertStore activeAlertStore;
    private final String webhookSecret;

    public AlertWebhookController(ActiveAlertStore activeAlertStore,
                                  @Value("${monitoring.webhook.secret:}") String webhookSecret) {
        this.activeAlertStore = activeAlertStore;
        this.webhookSecret = webhookSecret;
    }

    @Operation(summary = "Grafana 알림 웹훅 수신")
    @PostMapping("/alerts")
    public ApiResponse<Void> receive(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody GrafanaWebhookRequest request
    ) {
        verifySecret(authorization);

        if (request.alerts() != null) {
            for (GrafanaWebhookRequest.Alert alert : request.alerts()) {
                applyAlert(alert);
            }
        }
        return ApiResponse.successEmpty();
    }

    // NOTE: firing/resolved 외 상태(예: pending)는 무시한다 — Grafana Cloud 웹훅이 현재 두 상태만
    //       전송하므로 의도된 동작이다. 재전송(replay) 보호는 두지 않는다(upsert/remove가 멱등).
    private void applyAlert(GrafanaWebhookRequest.Alert alert) {
        if (RESOLVED.equalsIgnoreCase(alert.status())) {
            if (alert.fingerprint() != null) {
                activeAlertStore.remove(alert.fingerprint());
            }
            return;
        }
        if (FIRING.equalsIgnoreCase(alert.status())) {
            activeAlertStore.upsert(toResponse(alert));
        }
    }

    private ActiveAlertResponse toResponse(GrafanaWebhookRequest.Alert alert) {
        return new ActiveAlertResponse(
                alert.fingerprint(),
                label(alert, "alertname"),
                label(alert, "severity"),
                annotation(alert, "summary"),
                alert.startsAt()
        );
    }

    private String label(GrafanaWebhookRequest.Alert alert, String key) {
        return (alert.labels() == null) ? null : alert.labels().get(key);
    }

    private String annotation(GrafanaWebhookRequest.Alert alert, String key) {
        return (alert.annotations() == null) ? null : alert.annotations().get(key);
    }

    private void verifySecret(String authorization) {
        // NOTE: 시크릿 미설정(공백)이면 모든 요청을 거부한다(안전 기본값).
        if (webhookSecret == null || webhookSecret.isBlank() || authorization == null) {
            throw new BusinessException(MonitoringErrorCode.INVALID_WEBHOOK_SECRET);
        }
        // NOTE: 인터넷에 노출되는 시크릿 검증이므로 타이밍 공격 방지를 위해 상수 시간 비교를 쓴다.
        byte[] provided = authorization.getBytes(StandardCharsets.UTF_8);
        byte[] expected = (BEARER_PREFIX + webhookSecret).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(provided, expected)) {
            throw new BusinessException(MonitoringErrorCode.INVALID_WEBHOOK_SECRET);
        }
    }
}
