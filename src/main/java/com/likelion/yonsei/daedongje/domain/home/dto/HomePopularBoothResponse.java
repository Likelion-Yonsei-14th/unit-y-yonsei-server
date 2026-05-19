package com.likelion.yonsei.daedongje.domain.home.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.PopularBoothSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Schema(description = "홈 오늘의 인기 부스 응답")
@Getter
@Builder
public class HomePopularBoothResponse {

    @Schema(description = "인기 순위", example = "1")
    private Integer rank;

    @Schema(description = "부스 ID", example = "12")
    private Long boothId;

    @Schema(description = "오늘 클릭 수", example = "37")
    private Long clickCount;

    @Schema(description = "부스 이름", example = "호프 한 잔")
    private String name;

    @Schema(description = "운영 단체명", example = "사회학과")
    private String organization;

    @Schema(description = "부스 소개", example = "시원한 음료와 안주를 판매합니다.")
    private String description;

    @Schema(description = "축제 일차", example = "2")
    private Integer date;

    @Schema(description = "운영 시작 시간", example = "11:00")
    private LocalTime openTime;

    @Schema(description = "운영 종료 시간", example = "20:00")
    private LocalTime closeTime;

    @Schema(description = "구역", example = "백양로")
    private BoothSector sector;

    @Schema(description = "섹터 내 부스 배치 번호", example = "7")
    private Integer location;

    @Schema(description = "운영 상태", example = "OPEN")
    private BoothStatus status;

    @Schema(description = "음식 부스 여부", example = "true")
    private Boolean isFood;

    @Schema(description = "예약 가능 여부", example = "true")
    private Boolean isReservable;

    @Schema(description = "대표 메뉴 카테고리 목록", example = "[\"치킨\", \"맥주\"]")
    private List<String> representativeMenus;

    @Schema(description = "현재 대기 팀 수", example = "2")
    private Long waitingCount;

    @Schema(description = "썸네일 이미지 URL (display_order=1 이미지)", example = "https://example.com/thumbnail.jpg")
    private String thumbnailUrl;

    public static HomePopularBoothResponse of(int rank, PopularBoothSummary summary,
                                              Booth booth, long waitingCount, String thumbnailUrl) {
        return HomePopularBoothResponse.builder()
                .rank(rank)
                .boothId(summary.getBoothId())
                .clickCount(summary.getClickCount())
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
                .isReservable(booth.getIsReservable())
                .representativeMenus(parseMenus(booth.getRepresentativeMenus()))
                .waitingCount(waitingCount)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private static List<String> parseMenus(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(menu -> !menu.isBlank())
                .toList();
    }
}
