package com.likelion.yonsei.daedongje.domain.performance.dto;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(description = "공연 일차", example = "1", nullable = true)
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

        @Schema(description = "공연 상태", example = "SCHEDULED", nullable = true)
        PerformanceStatus performanceStatus
) {
}
