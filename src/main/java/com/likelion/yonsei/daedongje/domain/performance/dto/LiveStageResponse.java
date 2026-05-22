package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.LiveStageSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "무대별 라이브 공연 응답")
public record LiveStageResponse(
        @Schema(description = "공연 출처 (MANUAL=수동 핀, AUTO=시간 자동)", example = "AUTO")
        LiveStageSource source,

        @Schema(description = "해당 무대에서 현재 진행 중인 공연")
        PerformanceCurrentResponse performance
) {
    public static LiveStageResponse of(LiveStageSource source, PerformanceCurrentResponse performance) {
        return new LiveStageResponse(source, performance);
    }
}
