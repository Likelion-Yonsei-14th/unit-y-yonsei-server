package com.likelion.yonsei.daedongje.domain.reservation.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Schema(description = "내 예약 응답")
@Getter
@Builder
public class MyReservationResponse {

    @Schema(description = "예약 ID", example = "1")
    private Long id;

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

    @Schema(description = "내 앞 대기 팀 수 (PENDING 상태일 때만 유효, 그 외 0)", example = "3")
    private long aheadOfMe;

    @Schema(description = "부스 정보")
    private BoothInfo booth;

    @Schema(description = "부스 요약 정보")
    @Getter
    @Builder
    public static class BoothInfo {

        @Schema(description = "부스 ID", example = "3")
        private Long id;

        @Schema(description = "부스 이름", example = "멋사 핫도그")
        private String name;

        @Schema(description = "운영 단체명", example = "멋쟁이사자처럼 연세대")
        private String organization;

        @Schema(description = "축제 일차", example = "1")
        private Integer date;

        @Schema(description = "구역", example = "한글탑")
        private BoothSector sector;

        @Schema(description = "섹터 내 부스 배치 번호", example = "3")
        private Integer location;

        @Schema(description = "현재 대기 팀 수", example = "3")
        private long waitingCount;

        @Schema(description = "대표 메뉴 목록", example = "[\"치킨\", \"맥주\"]")
        private List<String> representativeMenus;

        @Schema(description = "썸네일 이미지 URL (없으면 null)", example = "https://example.com/thumbnail.jpg")
        private String thumbnailUrl;

        public static BoothInfo of(Booth booth, long waitingCount, String thumbnailUrl) {
            return BoothInfo.builder()
                    .id(booth.getId())
                    .name(booth.getName())
                    .organization(booth.getOrganization())
                    .date(booth.getDate())
                    .sector(booth.getSector())
                    .location(booth.getLocation())
                    .waitingCount(waitingCount)
                    .representativeMenus(parseMenus(booth.getRepresentativeMenus()))
                    .thumbnailUrl(thumbnailUrl)
                    .build();
        }

        private static List<String> parseMenus(String raw) {
            if (raw == null || raw.isBlank()) return List.of();
            return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        }
    }

    public static MyReservationResponse of(Reservation reservation, long aheadOfMe, BoothInfo boothInfo) {
        return MyReservationResponse.builder()
                .id(reservation.getId())
                .reservationNumber(reservation.getReservationNumber())
                .bookerName(reservation.getBookerName())
                .phoneNumber(reservation.getPhoneNumber())
                .partySize(reservation.getPartySize())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .aheadOfMe(aheadOfMe)
                .booth(boothInfo)
                .build();
    }
}
