package com.likelion.yonsei.daedongje.domain.performance.dto;

import java.time.LocalTime;

public record PerformanceCreateServiceRequest(
        String performanceName,
        Integer performanceDate,
        Long locationId,
        LocalTime startTime,
        LocalTime endTime
) {
}
