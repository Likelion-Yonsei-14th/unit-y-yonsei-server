package com.likelion.yonsei.daedongje.domain.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "부스 이미지 생성 요청")
public record BoothImageCreateRequest(

        @Schema(description = "부스 이미지 URL", example = "https://image.com/booth.png")
        @NotBlank
        String imageUrl,

        @Schema(description = "부스 이미지 표시 순서", example = "1")
        @NotNull
        @Min(value = 1, message = "부스 이미지 표시 순서는 1 이상이어야 합니다.")
        Integer displayOrder
) {
}
