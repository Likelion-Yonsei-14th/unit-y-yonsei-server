package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.yonsei.daedongje.domain.info.entity.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NoticeCreateRequest(
        @NotBlank(message = "title은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "title은 100자를 넘을 수 없습니다.")
        String title,

        @NotBlank(message = "content는 비어 있을 수 없습니다.")
        String content,

        @Size(max = 255, message = "instagramUrl은 255자를 넘을 수 없습니다.")
        String instagramUrl,

        Boolean hasImage,

        @Size(max = 1000, message = "imageUrl은 1000자를 넘을 수 없습니다.")
        String imageUrl,

        Boolean isPinned,

        @Schema(description = "Notice category", allowableValues = {"BLUERUN", "BOOTH", "PERFORMANCE", "OTHERS"})
        NoticeCategory category,

        Long performanceId,

        Long boothId,

        @JsonProperty("images")
        @Valid
        List<NoticeImageRequest> images
) {
}
