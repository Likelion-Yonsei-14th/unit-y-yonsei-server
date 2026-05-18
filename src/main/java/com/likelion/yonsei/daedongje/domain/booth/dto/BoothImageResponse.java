package com.likelion.yonsei.daedongje.domain.booth.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "부스 이미지 응답")
public class BoothImageResponse {

    @Schema(description = "부스 이미지 ID", example = "1")
    private Long id;

    @Schema(description = "부스 ID", example = "1")
    private Long boothId;

    @Schema(description = "부스 이미지 URL", example = "https://image.com/booth.png")
    private String imageUrl;

    @Schema(description = "부스 이미지 표시 순서", example = "1")
    private Integer displayOrder;

    public static BoothImageResponse from(BoothImage boothImage) {
        return BoothImageResponse.builder()
                .id(boothImage.getId())
                .boothId(boothImage.getBoothId())
                .imageUrl(boothImage.getImageUrl())
                .displayOrder(boothImage.getDisplayOrder())
                .build();
    }
}
