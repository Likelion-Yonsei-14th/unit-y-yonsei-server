package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.CheerMessageDisplayStatus;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;

import java.time.LocalDateTime;

public record PerformanceCheerMessageResponse(
        Long id,
        Long performanceId,
        String performanceName,
        Long setlistId,
        String singerName,
        String songTitle,
        String message,
        CheerMessageDisplayStatus displayStatus,
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
