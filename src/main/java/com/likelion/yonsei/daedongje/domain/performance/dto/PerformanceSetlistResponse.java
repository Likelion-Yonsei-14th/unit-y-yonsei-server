package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;

public record PerformanceSetlistResponse(
        Long id,
        Long performanceId,
        String songTitle,
        String singerName,
        Integer songOrder,
        String note
) {

    public static PerformanceSetlistResponse from(PerformanceSetlist performanceSetlist) {
        return new PerformanceSetlistResponse(
                performanceSetlist.getId(),
                performanceSetlist.getPerformance().getId(),
                performanceSetlist.getSongTitle(),
                performanceSetlist.getSingerName(),
                performanceSetlist.getSongOrder(),
                performanceSetlist.getNote()
        );
    }
}
