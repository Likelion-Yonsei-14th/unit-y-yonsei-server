package com.likelion.yonsei.daedongje.domain.booth.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "부스 운영 상태 변경 요청")
public record BoothStatusUpdateRequest(

        @Schema(description = "변경할 운영 상태 (OPEN / CLOSED / PREPARING)", example = "OPEN")
        @NotNull
        BoothStatus status
) {}
