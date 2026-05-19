package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Schema(description = "공연 타임테이블 응답")
@Getter
@Builder
public class PerformanceTimetableResponse {

    @Schema(description = "공연 ID", example = "1")
    private Long id;

    @Schema(description = "공연명", example = "연세 대동제 메인 공연")
    private String performanceName;

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

    @Schema(description = "공연 해시태그 1", example = "JPOP")
    private String hashtag1;

    @Schema(description = "공연 해시태그 2", example = "인디")
    private String hashtag2;

    @Schema(description = "공연 해시태그 3", example = "밴드")
    private String hashtag3;

    @Schema(description = "공연 유튜브 링크", example = "https://www.youtube.com/@yonsei")
    private String youtubeUrl;

    @Schema(description = "공연 인스타그램 링크", example = "https://www.instagram.com/yonsei")
    private String instagramUrl;

    @Schema(description = "공연 상태", example = "SCHEDULED")
    private PerformanceStatus performanceStatus;

    @Schema(description = "지도 위치 ID", example = "1")
    private Long locationId;

    @Schema(description = "지도 위치명", example = "노천극장")
    private String locationName;

    public static PerformanceTimetableResponse from(Performance performance) {
        MapLocation location = performance.getLocation();

        return PerformanceTimetableResponse.builder()
                .id(performance.getId())
                .performanceName(performance.getPerformanceName())
                .performanceDate(performance.getPerformanceDate())
                .startTime(performance.getStartTime())
                .endTime(performance.getEndTime())
                .performanceCategory(performance.getPerformanceCategory())
                .lineupName(performance.getLineupName())
                .hashtag1(performance.getHashtag1())
                .hashtag2(performance.getHashtag2())
                .hashtag3(performance.getHashtag3())
                .youtubeUrl(performance.getYoutubeUrl())
                .instagramUrl(performance.getInstagramUrl())
                .performanceStatus(performance.getPerformanceStatus())
                .locationId(location != null ? location.getId() : null)
                .locationName(location != null ? location.getLocationName() : null)
                .build();
    }
}
