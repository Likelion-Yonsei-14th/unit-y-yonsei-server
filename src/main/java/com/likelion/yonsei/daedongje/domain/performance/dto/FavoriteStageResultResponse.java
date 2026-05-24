package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가장 좋았던 무대 투표 결과")
public record FavoriteStageResultResponse(
        @Schema(description = "순위", example = "1")
        int rank,

        @Schema(description = "셋리스트 ID", example = "10")
        Long setlistId,

        @Schema(description = "곡명", example = "여름밤의 꿈")
        String songTitle,

        @Schema(description = "가수 또는 공연팀명", example = "밴드 X")
        String singerName,

        @Schema(description = "곡 순서", example = "1")
        Integer songOrder,

        @Schema(description = "득표 수", example = "45")
        long voteCount,

        @Schema(description = "득표율입니다. 소수점 한 자리까지 반환합니다.", example = "35.4")
        double voteRate
) {

    public static FavoriteStageResultResponse of(
            int rank,
            PerformanceSetlist setlist,
            long voteCount,
            long totalVoteCount
    ) {
        return new FavoriteStageResultResponse(
                rank,
                setlist.getId(),
                setlist.getSongTitle(),
                setlist.getSingerName(),
                setlist.getSongOrder(),
                voteCount,
                calculateVoteRate(voteCount, totalVoteCount)
        );
    }

    private static double calculateVoteRate(long voteCount, long totalVoteCount) {
        if (totalVoteCount == 0) {
            return 0.0;
        }
        return Math.round(((double) voteCount / totalVoteCount * 100.0) * 10.0) / 10.0;
    }
}
