package com.likelion.yonsei.daedongje.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "부스별 예약 현황 요약 응답")
@Getter
@Builder
public class ReservationSummaryResponse {

    @Schema(description = "예약이 1건 이상 존재하는 부스별 현황 목록 (0건 부스는 생략)")
    private List<BoothSummary> booths;

    @Schema(description = "조회 범위 전체 부스의 합산 현황")
    private Totals totals;

    public static ReservationSummaryResponse of(List<BoothSummary> booths, Totals totals) {
        return ReservationSummaryResponse.builder()
                .booths(booths)
                .totals(totals)
                .build();
    }

    @Schema(description = "부스별 예약 현황")
    @Getter
    @Builder
    public static class BoothSummary {

        @Schema(description = "부스 ID", example = "1")
        private Long boothId;

        @Schema(description = "대기 중(PENDING) 예약 수", example = "12")
        private long pending;

        @Schema(description = "입장 완료(CONFIRMED) 예약 수", example = "30")
        private long confirmed;

        @Schema(description = "취소(CANCELLED) 예약 수", example = "3")
        private long cancelled;

        @Schema(description = "부스 전체 예약 수 (pending + confirmed + cancelled)", example = "45")
        private long total;
    }

    @Schema(description = "전체 부스 합산 현황")
    @Getter
    @Builder
    public static class Totals {

        @Schema(description = "대기 중 예약 총합", example = "17")
        private long pending;

        @Schema(description = "입장 완료 예약 총합", example = "38")
        private long confirmed;

        @Schema(description = "취소 예약 총합", example = "4")
        private long cancelled;

        @Schema(description = "전체 예약 총합", example = "63")
        private long total;
    }
}
