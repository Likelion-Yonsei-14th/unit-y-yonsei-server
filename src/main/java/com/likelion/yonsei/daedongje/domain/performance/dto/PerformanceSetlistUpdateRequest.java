package com.likelion.yonsei.daedongje.domain.performance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerformanceSetlistUpdateRequest(
        @Pattern(regexp = ".*\\S.*") @Size(max = 100) String songTitle,
        @Pattern(regexp = ".*\\S.*") @Size(max = 100) String singerName,
        @Min(1) Integer songOrder,
        @Size(max = 255) String note
) {
}
