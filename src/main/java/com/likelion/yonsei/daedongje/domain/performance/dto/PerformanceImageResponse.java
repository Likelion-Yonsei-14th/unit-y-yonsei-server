package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImageType;

public record PerformanceImageResponse(
        Long id,
        Long performanceId,
        String imageUrl,
        Integer imageOrder,
        PerformanceImageType imageType
) {

    public static PerformanceImageResponse from(PerformanceImage performanceImage) {
        return new PerformanceImageResponse(
                performanceImage.getId(),
                performanceImage.getPerformance().getId(),
                performanceImage.getImageUrl(),
                performanceImage.getImageOrder(),
                performanceImage.getImageType()
        );
    }
}
