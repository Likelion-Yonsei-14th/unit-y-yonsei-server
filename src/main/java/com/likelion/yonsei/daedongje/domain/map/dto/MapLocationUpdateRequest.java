package com.likelion.yonsei.daedongje.domain.map.dto;

import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "지도 위치 수정 요청")
public record MapLocationUpdateRequest(

        @Schema(description = "지도 위치 이름", example = "백양로 메인 스테이지", nullable = true)
        @Pattern(regexp = ".*\\S.*")
        @Size(max = 100)
        String locationName,

        @Schema(description = "구역", example = "A", nullable = true)
        @Pattern(regexp = ".*\\S.*")
        @Size(max = 10)
        String sector,

        @Schema(description = "지도 X 좌표", example = "123.4567", nullable = true)
        @Digits(integer = 6, fraction = 4)
        BigDecimal mapX,

        @Schema(description = "지도 Y 좌표", example = "45.6789", nullable = true)
        @Digits(integer = 6, fraction = 4)
        BigDecimal mapY,

        @Schema(description = "지도 표시 너비", example = "12.345", nullable = true)
        @PositiveOrZero
        @Digits(integer = 3, fraction = 3)
        BigDecimal width,

        @Schema(description = "지도 표시 높이", example = "6.789", nullable = true)
        @PositiveOrZero
        @Digits(integer = 3, fraction = 3)
        BigDecimal height,

        @Schema(description = "위치 타입", example = "STAGE", allowableValues = {"STAGE", "BOOTH", "ENTRANCE", "FACILITY", "OTHER"}, nullable = true)
        MapLocationType locationType,

        @Schema(description = "노출 순서", example = "1", nullable = true)
        Integer displayOrder,

        @Schema(description = "노출 상태", example = "VISIBLE", allowableValues = {"VISIBLE", "HIDDEN"}, nullable = true)
        MapDisplayStatus displayStatus
) {
}
