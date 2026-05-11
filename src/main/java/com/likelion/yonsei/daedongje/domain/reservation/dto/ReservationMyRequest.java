package com.likelion.yonsei.daedongje.domain.reservation.dto;

import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = """
        본인 예약 조회 요청.
        전화번호·PIN 등 민감 정보가 URL(서버 로그·브라우저 히스토리·Referer 헤더)에 노출되는 것을
        방지하기 위해 GET 쿼리 파라미터 대신 POST + Request Body 방식을 사용합니다.
        """)
public record ReservationMyRequest(

        @Schema(description = "예약자 이름", example = "홍길동")
        @NotBlank
        String bookerName,

        @Schema(description = "연락처", example = "010-1234-5678")
        @NotBlank
        String phoneNumber,

        @Schema(
                description = "예약 조회용 PIN 4자리. PIN 없이 예약한 경우 생략 가능.",
                example = "1234",
                nullable = true
        )
        @Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
        String pin,

        @Schema(
                description = "예약 상태 필터. 생략 시 전체 조회.",
                example = "PENDING",
                allowableValues = {"PENDING", "CONFIRMED", "CANCELLED"},
                nullable = true
        )
        ReservationStatus status
) {}
