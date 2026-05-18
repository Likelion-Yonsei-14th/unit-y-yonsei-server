package com.likelion.yonsei.daedongje.domain.booth.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Schema(description = "예약 가능 부스 응답")
@Getter
@Builder
public class ReservableBoothResponse {

    @Schema(description = "부스 ID", example = "1")
    private Long id;

    @Schema(description = "부스 이름", example = "멋사 핫도그")
    private String name;

    @Schema(description = "운영 단체명", example = "멋쟁이사자처럼 연세대")
    private String organization;

    @Schema(description = "부스 소개", example = "맛있는 핫도그를 판매합니다.")
    private String description;

    @Schema(description = "축제 일차", example = "2")
    private Integer date;

    @Schema(description = "운영 시작 시간", example = "11:00")
    private LocalTime openTime;

    @Schema(description = "운영 종료 시간", example = "20:00")
    private LocalTime closeTime;

    @Schema(description = "구역", example = "백양로")
    private BoothSector sector;

    @Schema(description = "섹터 내 부스 배치 번호", example = "3")
    private Integer location;

    @Schema(description = "운영 상태", example = "OPEN")
    private BoothStatus status;

    @Schema(description = "음식 부스 여부", example = "true")
    private Boolean isFood;

    @Schema(description = "현재 대기 팀 수", example = "5")
    private long waitingCount;

    public static ReservableBoothResponse of(Booth booth, long waitingCount) {
        return ReservableBoothResponse.builder()
                .id(booth.getId())
                .name(booth.getName())
                .organization(booth.getOrganization())
                .description(booth.getDescription())
                .date(booth.getDate())
                .openTime(booth.getOpenTime())
                .closeTime(booth.getCloseTime())
                .sector(booth.getSector())
                .location(booth.getLocation())
                .status(booth.getStatus())
                .isFood(booth.getIsFood())
                .waitingCount(waitingCount)
                .build();
    }
}
