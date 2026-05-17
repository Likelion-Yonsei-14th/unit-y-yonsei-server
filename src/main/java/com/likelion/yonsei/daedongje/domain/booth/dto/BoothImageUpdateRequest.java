package com.likelion.yonsei.daedongje.domain.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "부스 이미지 수정 요청")
public record BoothImageUpdateRequest(

        @Schema(description = "부스 이미지 URL", example = "https://image.com/booth-updated.png")
        @Pattern(regexp = ".*\\S.*", message = "부스 이미지 URL은 공백만으로 입력할 수 없습니다.")
        String imageUrl,

        @Schema(description = "부스 이미지 표시 순서", example = "2")
        Integer displayOrder
) {
}
