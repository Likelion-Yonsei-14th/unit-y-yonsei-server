package com.likelion.yonsei.daedongje.domain.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "라이브 공연 지정/해제 요청")
public record LivePerformanceUpdateRequest(

        @Schema(description = "라이브로 지정할 공연 ID. null 이면 라이브 해제", example = "12")
        Long performanceId
) {
}
