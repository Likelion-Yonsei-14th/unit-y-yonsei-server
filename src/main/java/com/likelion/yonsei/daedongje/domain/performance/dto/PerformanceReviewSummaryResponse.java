package com.likelion.yonsei.daedongje.domain.performance.dto;

import java.util.List;

public record PerformanceReviewSummaryResponse(
        Long performanceId,
        String performanceName,
        long totalVoteCount,
        List<FavoriteStageResultResponse> favoriteStageResults
) {
}
