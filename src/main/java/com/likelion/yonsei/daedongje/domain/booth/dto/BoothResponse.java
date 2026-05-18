package com.likelion.yonsei.daedongje.domain.booth.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Schema(description = "부스 응답")
@Getter
@Builder
public class BoothResponse {

    @Schema(description = "부스 ID", example = "1")
    private Long id;

    @Schema(description = "어드민 ID", example = "1")
    private Long adminId;

    @Schema(description = "부스 이름", example = "멋사 핫도그")
    private String name;

    @Schema(description = "운영 단체명", example = "멋쟁이사자처럼 연세대")
    private String organization;

    @Schema(description = "부스 소개", example = "맛있는 핫도그를 판매합니다.")
    private String description;

    @Schema(description = "축제 일차", example = "1")
    private Integer date;

    @Schema(description = "운영 시작 시간", example = "11:00")
    private LocalTime openTime;

    @Schema(description = "운영 종료 시간", example = "20:00")
    private LocalTime closeTime;

    @Schema(description = "구역", example = "A")
    private BoothSector sector;

    @Schema(description = "섹터 내 부스 배치 번호 (예: 한글탑 1~20번 중 하나)", example = "3")
    private Integer location;

    @Schema(description = "운영 상태", example = "OPEN")
    private BoothStatus status;

    @Schema(description = "음식 부스 여부", example = "true")
    private Boolean isFood;

    @Schema(description = "인스타그램 계정 URL", example = "https://instagram.com/example")
    private String instagram;

    @Schema(description = "예약 가능 여부", example = "true")
    private Boolean isReservable;

    @Schema(description = "계좌 정보", example = "카카오뱅크 1234-5678")
    private String account;

    @Schema(description = "지도 위치 엔티티 ID", example = "10")
    private Long locationId;

    @Schema(description = "부스 프로필 작성 완료 여부. organization·date·openTime·closeTime·sector·location 이 모두 입력된 경우 true.", example = "false")
    private boolean profileComplete;

    @Schema(description = "대표 메뉴 카테고리 목록", example = "[\"치킨\", \"맥주\"]")
    private List<String> representativeMenus;

    @Schema(description = "현재 대기 팀 수", example = "3")
    private long waitingCount;

    @Schema(description = "썸네일 이미지 URL (display_order=1 이미지)", example = "https://example.com/thumbnail.jpg")
    private String thumbnailUrl;

    public static BoothResponse from(Booth booth) {
        return of(booth, 0L, null);
    }

    public static BoothResponse of(Booth booth, long waitingCount, String thumbnailUrl) {
        return BoothResponse.builder()
                .id(booth.getId())
                .adminId(booth.getAdminId())
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
                .instagram(booth.getInstagram())
                .isReservable(booth.getIsReservable())
                .account(booth.getAccount())
                .locationId(booth.getLocationId())
                .profileComplete(booth.isProfileComplete())
                .representativeMenus(parseMenus(booth.getRepresentativeMenus()))
                .waitingCount(waitingCount)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private static List<String> parseMenus(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
