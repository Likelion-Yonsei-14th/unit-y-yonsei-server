package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.yonsei.daedongje.domain.info.entity.BarrierFreeInfo;

public record BarrierFreeInfoResponse(
        Long id,
        String title,
        String content,
        @JsonProperty("guide_map_image_url")
        String guideMapImageUrl,
        @JsonProperty("facility_type")
        String facilityType,
        @JsonProperty("display_order")
        Integer displayOrder,
        @JsonProperty("map_location_id")
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
