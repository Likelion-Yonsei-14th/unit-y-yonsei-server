package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.yonsei.daedongje.domain.info.entity.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NoticeUpdateRequest(
        @NotBlank(message = "title must not be blank.")
        @Size(max = 100, message = "title must be 100 characters or fewer.")
        String title,

        @NotBlank(message = "content must not be blank.")
        String content,

        @Size(max = 255, message = "instagramUrl must be 255 characters or fewer.")
        String instagramUrl,

        @NotNull(message = "hasImage is required.")
        Boolean hasImage,

        @Size(max = 1000, message = "imageUrl must be 1000 characters or fewer.")
        String imageUrl,

        @NotNull(message = "isPinned is required.")
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
