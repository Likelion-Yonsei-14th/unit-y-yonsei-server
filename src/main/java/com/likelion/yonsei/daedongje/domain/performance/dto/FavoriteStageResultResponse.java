package com.likelion.yonsei.daedongje.domain.performance.dto;

public record FavoriteStageResultResponse(
        int rank,
        Long setlistId,
        String songTitle,
        long voteCount,
        double voteRate
) {
}
