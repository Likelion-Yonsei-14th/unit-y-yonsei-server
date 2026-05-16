package com.likelion.yonsei.daedongje.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 예약 정보 수정 요청")
public record ReservationUpdateRequest(

        @Schema(description = "현재 예약자 명 (소유권 확인용)", example = "홍길동")
        @NotBlank
        @Size(max = 20)
        String bookerName,

        @Schema(description = "현재 연락처 (소유권 확인용)", example = "010-1234-5678")
        @NotBlank
        @Size(max = 20)
        String phoneNumber,

        @Schema(description = "예약 조회용 비밀번호 4자리 (PIN 설정 시 필수)", example = "1234")
        @Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
        String pin,

        @Schema(description = "변경할 예약자 명 (생략 시 유지)", example = "김철수")
        @Size(max = 20)
        String newBookerName,

        @Schema(description = "변경할 연락처 (생략 시 유지)", example = "010-9876-5432")
        @Size(max = 20)
        String newPhoneNumber,

        @Schema(description = "변경할 인원 수 (생략 시 유지)", example = "3")
        @Min(1)
        Integer newPartySize
) {}