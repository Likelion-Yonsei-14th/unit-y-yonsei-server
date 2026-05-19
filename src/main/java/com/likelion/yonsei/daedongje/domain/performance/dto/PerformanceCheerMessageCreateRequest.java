package com.likelion.yonsei.daedongje.domain.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerformanceCheerMessageCreateRequest(
        Long setlistId,

        @NotBlank(message = "Cheer message content is required.")
        @Size(max = 255, message = "Cheer message content must be 255 characters or less.")
        String message
) {
}
