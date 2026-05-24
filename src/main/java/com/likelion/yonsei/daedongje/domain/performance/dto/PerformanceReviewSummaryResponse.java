package com.likelion.yonsei.daedongje.domain.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공연 후기 수합 요약 응답")
public record PerformanceReviewSummaryResponse(
        @Schema(description = "공연 ID", example = "1")
        Long performanceId,

        @Schema(description = "공연명", example = "밴드 X")
        String performanceName,

        @Schema(description = "셋리스트를 선택한 총 투표 수", example = "127")
        long totalVoteCount,

        @Schema(description = "가장 좋았던 무대 투표 결과 목록")
        List<FavoriteStageResultResponse> favoriteStageResults
) {
}
