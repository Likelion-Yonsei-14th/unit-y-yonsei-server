package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.yonsei.daedongje.domain.info.entity.LostItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분실물 응답")
public record LostItemResponse(
        Long id,
        String name,
        String location,
        String date,

        @JsonProperty("has_image")
        boolean hasImage,

        String description,

        @JsonProperty("image_url")
        String imageUrl,

        String status,

        @JsonProperty("found_location_id")
        Long foundLocationId,

        @JsonProperty("storage_location_id")
        Long storageLocationId
) {

    public static LostItemResponse from(LostItem lostItem) {
        return new LostItemResponse(
                lostItem.getId(),
                lostItem.getName(),
                lostItem.getLocation(),
                lostItem.getCreatedAt().toLocalDate().toString(),
                lostItem.hasImage(),
                lostItem.getDescription(),
                lostItem.getImageUrl(),
                lostItem.getStatus(),
                lostItem.getFoundLocationId(),
                lostItem.getStorageLocationId()
        );
    }
}
