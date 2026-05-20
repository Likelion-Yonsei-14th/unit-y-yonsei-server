package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@Schema(description = "공연 기본 정보 수정 요청")
public record PerformanceUpdateRequest(

        @Schema(description = "지도 위치 ID", example = "1", nullable = true)
        Long locationId,

        @Schema(description = "공연명", example = "연세 대동제 메인 공연", nullable = true)
        @Size(max = 100)
        String performanceName,

        @Schema(description = "공연 설명", example = "대동제 메인 무대 공연입니다.", nullable = true)
        String performanceDescription,

        @Schema(description = "공연 일차 (2~4 — 축제 일차 체계, bac-97 정합)", example = "2", nullable = true)
        @Min(2) @Max(4)
        Integer performanceDate,

        @Schema(description = "공연 시작 시간", example = "18:00", nullable = true)
        LocalTime startTime,

        @Schema(description = "공연 종료 시간", example = "20:00", nullable = true)
        LocalTime endTime,

        @Schema(description = "공연 구분", example = "ARTIST", allowableValues = {"ARTIST", "CLUB"}, nullable = true)
        PerformanceCategory performanceCategory,

        @Schema(description = "라인업명", example = "Lineup A", nullable = true)
        @Size(max = 100)
        String lineupName,

        @Schema(description = "공연 해시태그 1", example = "JPOP", nullable = true)
        @Size(max = 6)
        @Pattern(regexp = ".*\\S.*", message = "hashtag1은 공백만으로 입력할 수 없습니다.")
        String hashtag1,

        @Schema(description = "공연 해시태그 2", example = "인디", nullable = true)
        @Size(max = 6)
        @Pattern(regexp = ".*\\S.*", message = "hashtag2는 공백만으로 입력할 수 없습니다.")
        String hashtag2,

        @Schema(description = "공연 해시태그 3", example = "밴드", nullable = true)
        @Size(max = 6)
        @Pattern(regexp = ".*\\S.*", message = "hashtag3은 공백만으로 입력할 수 없습니다.")
        String hashtag3,

        @Schema(description = "공연 유튜브 링크", example = "https://www.youtube.com/@yonsei", nullable = true)
        @Size(max = 255)
        String youtubeUrl,

        @Schema(description = "공연 인스타그램 링크", example = "https://www.instagram.com/yonsei", nullable = true)
        @Size(max = 255)
        String instagramUrl,

        @Schema(description = "공연 상태", example = "SCHEDULED", nullable = true)
        PerformanceStatus performanceStatus
) {
}
