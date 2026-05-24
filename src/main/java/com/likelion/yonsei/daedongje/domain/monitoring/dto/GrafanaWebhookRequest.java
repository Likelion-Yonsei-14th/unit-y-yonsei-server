package com.likelion.yonsei.daedongje.domain.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Grafana Cloud 알림 웹훅 payload(필요 필드만). 미지의 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrafanaWebhookRequest(
        String status,
        List<Alert> alerts
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(
            String status,                 // firing / resolved
            Map<String, String> labels,    // alertname, severity, ...
            Map<String, String> annotations, // summary, description, ...
            String startsAt,
            String endsAt,
            String fingerprint
    ) {
    }
}
