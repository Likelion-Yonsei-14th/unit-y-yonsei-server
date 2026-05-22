package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeImageRequest(
        @JsonProperty("image_url")
        @NotBlank(message = "image_url은 비어 있을 수 없습니다.")
        @Size(max = 1000, message = "image_url은 1000자를 넘을 수 없습니다.")
        String imageUrl,

        @JsonProperty("display_order")
        @NotNull(message = "display_order는 필수입니다.")
        @Min(value = 1, message = "display_order는 1 이상이어야 합니다.")
        Integer displayOrder
) {
}
