package com.likelion.yonsei.daedongje.domain.reservation.dto;

import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "예약 생성 응답")
@Getter
@Builder
public class ReservationCreateResponse {

    @Schema(description = "예약 ID", example = "1")
    private Long id;

    @Schema(description = "부스 ID", example = "3")
    private Long boothId;

    @Schema(description = "부스명", example = "멋사 핫도그")
    private String boothName;

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

    @Schema(description = "현재 대기 팀 수 (본인 포함)", example = "5")
    private int waitingCount;

    @Schema(description = "예약 생성 시간")
    private LocalDateTime createdAt;

    public static ReservationCreateResponse of(Reservation reservation, int waitingCount) {
        return ReservationCreateResponse.builder()
                .id(reservation.getId())
                .boothId(reservation.getBooth().getId())
                .boothName(reservation.getBooth().getName())
                .reservationNumber(reservation.getReservationNumber())
                .bookerName(reservation.getBookerName())
                .phoneNumber(reservation.getPhoneNumber())
                .partySize(reservation.getPartySize())
                .status(reservation.getStatus())
                .waitingCount(waitingCount)
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
