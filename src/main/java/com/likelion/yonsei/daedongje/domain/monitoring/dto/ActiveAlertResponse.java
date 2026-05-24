package com.likelion.yonsei.daedongje.domain.monitoring.dto;

/**
 * 현재 발생 중인 알림 1건(관리자 표시용). Redis 해시에 JSON으로 저장된다.
 */
public record ActiveAlertResponse(
        String fingerprint,
        String name,        // Grafana label: alertname
        String severity,    // Grafana label: severity
        String summary,     // Grafana annotation: summary
        String startsAt     // ISO-8601 문자열
) {
}
