package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NoticeUpdateRequest(
        @NotBlank(message = "title은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "title은 100자를 넘을 수 없습니다.")
        String title,

        @NotBlank(message = "content는 비어 있을 수 없습니다.")
        String content,

        @Size(max = 255, message = "instagramUrl은 255자를 넘을 수 없습니다.")
        String instagramUrl,

        @NotNull(message = "hasImage는 필수입니다.")
        Boolean hasImage,

        @Size(max = 1000, message = "imageUrl은 1000자를 넘을 수 없습니다.")
        String imageUrl,

        @NotNull(message = "isPinned는 필수입니다.")
        Boolean isPinned,

        @Size(max = 50, message = "category는 50자를 넘을 수 없습니다.")
        String category,

        Long performanceId,

        Long boothId,

        @JsonProperty("images")
        @Valid
        List<NoticeImageRequest> images
) {
}
