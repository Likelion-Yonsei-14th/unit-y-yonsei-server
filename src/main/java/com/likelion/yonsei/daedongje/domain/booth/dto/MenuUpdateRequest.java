package com.likelion.yonsei.daedongje.domain.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 메뉴 수정 요청 DTO
@Schema(description = "메뉴 수정 요청")
public record MenuUpdateRequest(

                @Schema(description = "메뉴 이름", example = "치즈 핫도그") @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "메뉴 이름은 공백만으로 입력할 수 없습니다.") String name,

                @Schema(description = "메뉴 설명", example = "치즈가 추가된 핫도그") @Size(max = 255) String description,

                @Schema(description = "메뉴 가격", example = "6000") @Min(0) Integer price,

                @Schema(description = "메뉴 이미지 URL", example = "https://image.com/menu.png") String imageUrl,

                @Schema(description = "품절 여부", example = "true") Boolean isSoldOut,

                @Schema(description = "메뉴 표시 순서", example = "2") Integer displayOrder) {
}
