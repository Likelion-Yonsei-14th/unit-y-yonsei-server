package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;

import java.time.LocalDateTime;

public record PerformanceReviewResponse(
        Long reviewId,
        Long setlistId,
        String songTitle,
        String message,
        LocalDateTime createdAt
) {

    public static PerformanceReviewResponse from(PerformanceCheerMessage message) {
        PerformanceSetlist setlist = message.getSetlist();
        return new PerformanceReviewResponse(
                message.getId(),
                setlist != null ? setlist.getId() : null,
                setlist != null ? setlist.getSongTitle() : null,
                message.getMessage(),
                message.getCreatedAt()
        );
    }
}
