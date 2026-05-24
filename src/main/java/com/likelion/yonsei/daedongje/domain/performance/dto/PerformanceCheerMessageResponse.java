package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.CheerMessageDisplayStatus;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공연 응원 메시지 응답")
public record PerformanceCheerMessageResponse(
        @Schema(description = "응원 메시지 ID", example = "1")
        Long id,

        @Schema(description = "공연 ID", example = "1")
        Long performanceId,

        @Schema(description = "공연명", example = "메인 스테이지")
        String performanceName,

        @Schema(description = "셋리스트 ID", example = "10")
        Long setlistId,

        @Schema(description = "가수 또는 공연팀명", example = "밴드 X")
        String singerName,

        @Schema(description = "곡명", example = "여름밤의 꿈")
        String songTitle,

        @Schema(description = "응원 메시지 내용", example = "무대가 정말 좋았어요.")
        String message,

        @Schema(description = "응원 메시지 노출 상태", example = "VISIBLE")
        CheerMessageDisplayStatus displayStatus,

        @Schema(description = "작성 시각")
        LocalDateTime createdAt
) {

    public static PerformanceCheerMessageResponse from(PerformanceCheerMessage cheerMessage) {
        Performance performance = cheerMessage.getPerformance();
        PerformanceSetlist setlist = cheerMessage.getSetlist();

        return new PerformanceCheerMessageResponse(
                cheerMessage.getId(),
                performance.getId(),
                performance.getPerformanceName(),
                setlist != null ? setlist.getId() : null,
                setlist != null ? setlist.getSingerName() : null,
                setlist != null ? setlist.getSongTitle() : null,
                cheerMessage.getMessage(),
                cheerMessage.getDisplayStatus(),
                cheerMessage.getCreatedAt()
        );
    }
}
