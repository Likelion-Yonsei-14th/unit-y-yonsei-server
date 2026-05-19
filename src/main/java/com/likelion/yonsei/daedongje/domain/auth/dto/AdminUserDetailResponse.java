package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminUserDetailResponse {

    private Long id;
    private String loginId;
    private String organization;
    private String role;
    private String status;
    private String representativeName;
    private String representativePhone;
    private String memo;
    @Schema(description = "정보 작성 완료 여부", example = "false")
    private boolean infoCompleted;
    @Schema(description = "연동된 부스 상세 목록")
    private List<LinkedBoothDetail> linkedBooths;
    @Schema(description = "연동된 공연 상세 정보 (계정당 최대 1개)")
    private LinkedPerformanceDetail linkedPerformance;

    public static AdminUserDetailResponse from(
            AdminUser adminUser,
            boolean infoCompleted,
            List<Booth> linkedBooths,
            Performance linkedPerformance
    ) {
        return new AdminUserDetailResponse(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getOrganization(),
                adminUser.getRole().name(),
                adminUser.getStatus().name(),
                adminUser.getRepresentativeName(),
                adminUser.getRepresentativePhone(),
                adminUser.getMemo(),
                infoCompleted,
                linkedBooths == null ? null : linkedBooths.stream()
                        .map(LinkedBoothDetail::from)
                        .toList(),
                linkedPerformance == null ? null : LinkedPerformanceDetail.from(linkedPerformance)
        );
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LinkedBoothDetail {
        @Schema(description = "부스 ID", example = "1")
        private Long id;
        @Schema(description = "부스 이름", example = "멋사 핫도그")
        private String name;
        @Schema(description = "운영 단체명", example = "멋쟁이사자처럼 연세대")
        private String organization;
        @Schema(description = "부스 운영 날짜", example = "1")
        private Integer date;
        @Schema(description = "운영 시작 시간", example = "11:00")
        private LocalTime openTime;
        @Schema(description = "운영 종료 시간", example = "20:00")
        private LocalTime closeTime;
        @Schema(description = "부스 구역", example = "A")
        private BoothSector sector;
        @Schema(description = "구역 내 위치", example = "3")
        private Integer location;
        @Schema(description = "부스 상태", example = "PREPARING")
        private BoothStatus status;

        public static LinkedBoothDetail from(Booth booth) {
            return new LinkedBoothDetail(
                    booth.getId(),
                    booth.getName(),
                    booth.getOrganization(),
                    booth.getDate(),
                    booth.getOpenTime(),
                    booth.getCloseTime(),
                    booth.getSector(),
                    booth.getLocation(),
                    booth.getStatus()
            );
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LinkedPerformanceDetail {
        @Schema(description = "공연 ID", example = "1")
        private Long id;
        @Schema(description = "공연 이름", example = "AKARAKA 밴드")
        private String performanceName;
        @Schema(description = "공연 설명", example = "메인 공연")
        private String performanceDescription;
        @Schema(description = "공연 일차", example = "2")
        private Integer performanceDate;
        @Schema(description = "시작 시간", example = "18:00")
        private LocalTime startTime;
        @Schema(description = "종료 시간", example = "19:00")
        private LocalTime endTime;
        @Schema(description = "공연 카테고리", example = "ARTIST")
        private PerformanceCategory performanceCategory;
        @Schema(description = "라인업 이름", example = "Lineup A")
        private String lineupName;
        @Schema(description = "공연 상태", example = "SCHEDULED")
        private PerformanceStatus performanceStatus;

        public static LinkedPerformanceDetail from(Performance performance) {
            return new LinkedPerformanceDetail(
                    performance.getId(),
                    performance.getPerformanceName(),
                    performance.getPerformanceDescription(),
                    performance.getPerformanceDate(),
                    performance.getStartTime(),
                    performance.getEndTime(),
                    performance.getPerformanceCategory(),
                    performance.getLineupName(),
                    performance.getPerformanceStatus()
            );
        }
    }
}
