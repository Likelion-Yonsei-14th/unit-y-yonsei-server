package com.likelion.yonsei.daedongje.domain.reservation.dto;

import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "예약 응답")
@Getter
@Builder
public class ReservationResponse {

    @Schema(description = "예약 ID", example = "1")
    private Long id;

    @Schema(description = "부스 ID", example = "3")
    private Long boothId;

    @Schema(description = "부스별 예약 순번", example = "5")
    private Integer reservationNumber;

    @Schema(description = "예약자 명", example = "홍길동")
    private String bookerName;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "인원 수", example = "2")
    private Integer partySize;

    @Schema(description = "예약 상태", example = "PENDING")
    private ReservationStatus status;

    @Schema(description = "예약 생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간")
    private LocalDateTime updatedAt;

    public static ReservationResponse from(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .boothId(reservation.getBooth().getId())
                .reservationNumber(reservation.getReservationNumber())
                .bookerName(reservation.getBookerName())
                .phoneNumber(reservation.getPhoneNumber())
                .partySize(reservation.getPartySize())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
