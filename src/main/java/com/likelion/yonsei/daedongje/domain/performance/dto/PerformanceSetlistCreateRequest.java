package com.likelion.yonsei.daedongje.domain.performance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PerformanceSetlistCreateRequest(
        @NotBlank @Size(max = 100) String songTitle,
        @NotBlank @Size(max = 100) String singerName,
        @NotNull @Min(1) Integer songOrder,
        @Size(max = 255) String note
) {
}
