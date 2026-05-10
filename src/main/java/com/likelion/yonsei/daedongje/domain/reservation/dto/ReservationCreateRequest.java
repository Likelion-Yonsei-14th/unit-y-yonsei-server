package com.likelion.yonsei.daedongje.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;


@Schema(description = "예약 생성 요청")
public record ReservationCreateRequest(

        @Schema(description = "예약자 명", example = "홍길동")
        @NotBlank
        @Size(max = 20)
        String bookerName,

        @Schema(description = "연락처", example = "010-1234-5678")
        @NotBlank
        @Size(max = 20)
        String phoneNumber,

        @Schema(description = "인원 수", example = "2")
        @NotNull
        @Min(1)
        Integer partySize,

        @Schema(description = "예약 조회용 비밀번호 4자리 (선택)", example = "1234")
        @Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
        String pin
) {}
