package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Schema(description = "현재 공연 응답")
@Getter
@Builder
public class PerformanceCurrentResponse {

    @Schema(description = "공연 ID", example = "1")
    private Long id;

    @Schema(description = "공연명", example = "연세 대동제 메인 공연")
    private String performanceName;

    @Schema(description = "공연 시작 시간", example = "18:00")
    private LocalTime startTime;

    @Schema(description = "공연 종료 시간", example = "20:00")
    private LocalTime endTime;

    @Schema(description = "공연 상태", example = "ONGOING")
    private PerformanceStatus performanceStatus;

    @Schema(description = "공연 구분", example = "ARTIST", allowableValues = {"ARTIST", "CLUB"})
    private PerformanceCategory performanceCategory;

    @Schema(description = "지도 위치 ID", example = "1")
    private Long locationId;

    @Schema(description = "지도 위치명", example = "노천극장")
    private String locationName;

    public static PerformanceCurrentResponse from(Performance performance) {
        MapLocation location = performance.getLocation();

        return PerformanceCurrentResponse.builder()
                .id(performance.getId())
                .performanceName(performance.getPerformanceName())
                .startTime(performance.getStartTime())
                .endTime(performance.getEndTime())
                .performanceStatus(performance.getPerformanceStatus())
                .performanceCategory(performance.getPerformanceCategory())
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getLocationName() : null)
                .build();
    }
}
