package com.likelion.yonsei.daedongje.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예약 취소 요청")
public record ReservationCancelRequest(

        @Schema(description = "예약 취소 사유", example = "일정 변경으로 인한 취소")
        String cancelReason
) {}
