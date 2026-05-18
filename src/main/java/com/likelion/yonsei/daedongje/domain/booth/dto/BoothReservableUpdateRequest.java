package com.likelion.yonsei.daedongje.domain.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "부스 예약 접수 On/Off 요청")
public record BoothReservableUpdateRequest(

        @Schema(description = "예약 접수 활성화 여부 (true = 접수 중 / false = 접수 중단)", example = "false")
        @NotNull
        Boolean isReservable
) {}
