package com.likelion.yonsei.daedongje.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 예약 취소 요청")
public record ReservationUserCancelRequest(

        @Schema(description = "예약자 명", example = "홍길동")
        @NotBlank
        @Size(max = 20)
        String bookerName,

        @Schema(description = "연락처", example = "010-1234-5678")
        @NotBlank
        @Size(max = 20)
        String phoneNumber,

        @Schema(description = "예약 조회용 비밀번호 4자리 (PIN 설정 시 필수)", example = "1234")
        String pin,

        @Schema(description = "예약 취소 사유", example = "일정 변경으로 인한 취소")
        String cancelReason
) {}
