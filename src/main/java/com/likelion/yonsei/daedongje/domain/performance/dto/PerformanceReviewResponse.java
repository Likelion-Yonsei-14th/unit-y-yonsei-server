package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공연 관객 후기 응답")
public record PerformanceReviewResponse(
        @Schema(description = "후기 ID", example = "1")
        Long reviewId,

        @Schema(description = "자동 부여된 관객 표시명", example = "관객 00001")
        String nickname,

        @Schema(description = "셋리스트 ID", example = "10")
        Long setlistId,

        @Schema(description = "곡명", example = "여름밤의 꿈")
        String songTitle,

        @Schema(description = "가수 또는 공연팀명", example = "밴드 X")
        String singerName,

        @Schema(description = "곡 순서", example = "1")
        Integer songOrder,

        @Schema(description = "후기 메시지", example = "무대가 정말 좋았어요.")
        String message,

        @Schema(description = "작성 시각")
        LocalDateTime createdAt
) {

    public static PerformanceReviewResponse from(PerformanceCheerMessage cheerMessage) {
        PerformanceSetlist setlist = cheerMessage.getSetlist();
        return new PerformanceReviewResponse(
                cheerMessage.getId(),
                generateAudienceNickname(cheerMessage.getId()),
                setlist != null ? setlist.getId() : null,
                setlist != null ? setlist.getSongTitle() : null,
                setlist != null ? setlist.getSingerName() : null,
                setlist != null ? setlist.getSongOrder() : null,
                cheerMessage.getMessage(),
                cheerMessage.getCreatedAt()
        );
    }

    private static String generateAudienceNickname(Long id) {
        return "관객 " + String.format("%05d", id);
    }
}
