package com.likelion.yonsei.daedongje.domain.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 메뉴 생성 요청 DTO
@Schema(description = "메뉴 생성 요청")
public record MenuCreateRequest(

                @Schema(description = "메뉴 이름", example = "핫도그") @NotBlank @Size(max = 255) String name,

                @Schema(description = "메뉴 설명", example = "맛있는 핫도그") @Size(max = 255) String description,

                @Schema(description = "메뉴 가격", example = "5000") @NotNull @Min(0) Integer price,

                @Schema(description = "메뉴 이미지 URL", example = "https://image.com/menu.png") String imageUrl,

                @Schema(description = "품절 여부", example = "false") Boolean isSoldOut,

                @Schema(description = "메뉴 표시 순서", example = "1") @NotNull Integer displayOrder) {
}