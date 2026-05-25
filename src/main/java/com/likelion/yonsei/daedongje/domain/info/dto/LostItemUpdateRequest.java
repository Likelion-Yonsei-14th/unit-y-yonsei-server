package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.yonsei.daedongje.domain.info.entity.LostItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LostItemUpdateRequest(
        @NotBlank(message = "name은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "name은 100자를 넘을 수 없습니다.")
        String name,

        @NotBlank(message = "location은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "location은 100자를 넘을 수 없습니다.")
        String location,

        String description,

        @JsonProperty("has_image")
        Boolean hasImage,

        @JsonProperty("image_url")
        @Size(max = 255, message = "image_url은 255자를 넘을 수 없습니다.")
        String imageUrl,

        @Schema(description = "분실물 상태. 생략 시 STORED 로 저장됩니다.", allowableValues = {"STORED", "CLAIMED"})
        LostItemStatus status,

        @JsonProperty("found_location_id")
        Long foundLocationId,

        @JsonProperty("storage_location_id")
        Long storageLocationId
) {
}
