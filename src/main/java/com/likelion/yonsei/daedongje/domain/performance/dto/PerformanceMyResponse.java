package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "공연 어드민 본인 공연 응답")
@Getter
@Builder
public class PerformanceMyResponse {

    @Schema(description = "공연 ID", example = "1")
    private Long id;

    @Schema(description = "지도 위치 ID", example = "1")
    private Long locationId;

    @Schema(description = "지도 위치명", example = "노천극장")
    private String locationName;

    @Schema(description = "공연명", example = "연세 대동제 메인 공연")
    private String performanceName;

    @Schema(description = "공연 설명", example = "대동제 메인 무대 공연입니다.")
    private String performanceDescription;

    @Schema(description = "공연 일차", example = "1")
    private Integer performanceDate;

    @Schema(description = "공연 시작 시간", example = "18:00")
    private LocalTime startTime;

    @Schema(description = "공연 종료 시간", example = "20:00")
    private LocalTime endTime;

    @Schema(description = "공연 구분", example = "ARTIST", allowableValues = {"ARTIST", "CLUB"})
    private PerformanceCategory performanceCategory;

    @Schema(description = "라인업명", example = "Lineup A")
    private String lineupName;

    @Schema(description = "공연 상태", example = "SCHEDULED")
    private PerformanceStatus performanceStatus;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static PerformanceMyResponse from(Performance performance) {
        MapLocation location = performance.getLocation();

        return PerformanceMyResponse.builder()
                .id(performance.getId())
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getLocationName() : null)
                .performanceName(performance.getPerformanceName())
                .performanceDescription(performance.getPerformanceDescription())
                .performanceDate(performance.getPerformanceDate())
                .startTime(performance.getStartTime())
                .endTime(performance.getEndTime())
                .performanceCategory(performance.getPerformanceCategory())
                .lineupName(performance.getLineupName())
                .performanceStatus(performance.getPerformanceStatus())
                .createdAt(performance.getCreatedAt())
                .updatedAt(performance.getUpdatedAt())
                .build();
    }
}
