package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImageType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PerformanceImageCreateRequest(
        @NotBlank String imageUrl,
        @NotNull @Min(1) Integer imageOrder,
        @NotNull PerformanceImageType imageType
) {
}
