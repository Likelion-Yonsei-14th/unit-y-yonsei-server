package com.likelion.yonsei.daedongje.domain.reservation.dto;

import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "어드민 예약 상태 변경 요청")
public record ReservationAdminStatusRequest(

        @Schema(
                description = "변경할 예약 상태. CONFIRMED(입장 완료) 또는 CANCELLED(취소)만 허용.",
                example = "CONFIRMED",
                allowableValues = {"CONFIRMED", "CANCELLED"}
        )
        @NotNull
        ReservationStatus status
) {}
