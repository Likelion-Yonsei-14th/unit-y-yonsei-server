package com.likelion.yonsei.daedongje.domain.monitoring.dto;

/**
 * 서버 라이브 상태 스냅샷. 모든 수치 필드는 해당 메트릭이 없으면 null(부분 실패 허용).
 */
public record SystemHealthResponse(
        String status,          // UP / DOWN / OUT_OF_SERVICE / UNKNOWN
        String version,         // 빌드 버전, nullable
        Long uptimeSeconds,     // nullable
        MemoryInfo heap,
        DbPoolInfo dbPool,
        Integer liveThreads,    // nullable
        Double cpuUsage         // 0.0~1.0, nullable
) {
    public record MemoryInfo(Long usedBytes, Long maxBytes, Double usedRatio) {
    }

    public record DbPoolInfo(Integer active, Integer idle, Integer pending, Integer max) {
    }
}
