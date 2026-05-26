package com.likelion.yonsei.daedongje.domain.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "공연 응원 메시지 등록 요청")
public record PerformanceCheerMessageCreateRequest(
        @Schema(description = "셋리스트 ID입니다. 가장 좋았던 곡을 선택한 경우 전달합니다.", example = "10")
        Long setlistId,

        @Schema(description = "응원 메시지 내용입니다.", example = "무대가 정말 좋았어요.")
        @NotBlank(message = "Cheer message content is required.")
        @Size(max = 255, message = "Cheer message content must be 255 characters or less.")
        String message
) {
}
