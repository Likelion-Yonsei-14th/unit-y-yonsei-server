package com.likelion.yonsei.daedongje.domain.info.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeUpdateRequest(
        @NotBlank(message = "title은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "title은 100자를 넘을 수 없습니다.")
        String title,

        @NotBlank(message = "content는 비어 있을 수 없습니다.")
        String content,

        @NotNull(message = "hasImage는 필수입니다.")
        Boolean hasImage,

        @Size(max = 255, message = "imageUrl은 255자를 넘을 수 없습니다.")
        String imageUrl,

        @NotNull(message = "isPinned는 필수입니다.")
        Boolean isPinned,

        @Size(max = 50, message = "category는 50자를 넘을 수 없습니다.")
        String category,

        Long performanceId,

        Long boothId
) {
}
