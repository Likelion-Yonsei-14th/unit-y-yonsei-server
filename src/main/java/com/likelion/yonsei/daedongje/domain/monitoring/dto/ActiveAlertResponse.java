package com.likelion.yonsei.daedongje.domain.monitoring.dto;

/**
 * 현재 발생 중인 알림 1건(관리자 표시용). Redis 해시에 JSON으로 저장된다.
 *
 * <p>NOTE: 이 record가 Redis 저장 JSON 스키마를 겸한다. 필드를 변경하면 기존 Redis 값
 * 역직렬화가 깨질 수 있으므로(스키마 진화), 변경 시 키 플러시 또는 마이그레이션이 필요하다.
 */
public record ActiveAlertResponse(
        String fingerprint,
        String name,        // Grafana label: alertname
        String severity,    // Grafana label: severity
        String summary,     // Grafana annotation: summary
        String startsAt     // ISO-8601 문자열
) {
}
