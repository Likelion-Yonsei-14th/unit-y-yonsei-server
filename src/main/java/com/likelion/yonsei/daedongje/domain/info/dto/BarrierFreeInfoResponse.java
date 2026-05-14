package com.likelion.yonsei.daedongje.domain.info.dto;

import com.likelion.yonsei.daedongje.domain.info.entity.BarrierFreeInfo;

public record BarrierFreeInfoResponse(
        Long id,
        String title,
        String content,
        String guideMapImageUrl,
        String facilityType,
        Integer displayOrder,
        Long mapLocationId
) {
    public static BarrierFreeInfoResponse from(BarrierFreeInfo barrierFreeInfo) {
        return new BarrierFreeInfoResponse(
                barrierFreeInfo.getId(),
                barrierFreeInfo.getTitle(),
                barrierFreeInfo.getContent(),
                barrierFreeInfo.getGuideMapImageUrl(),
                barrierFreeInfo.getFacilityType(),
                barrierFreeInfo.getDisplayOrder(),
                barrierFreeInfo.getMapLocationId()
        );
    }
}
