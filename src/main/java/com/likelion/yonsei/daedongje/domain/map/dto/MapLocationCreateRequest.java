package com.likelion.yonsei.daedongje.domain.map.dto;

import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "지도 위치 생성 요청")
public record MapLocationCreateRequest(

        @Schema(description = "지도 위치 이름", example = "백양로 메인 스테이지")
        @NotBlank
        @Size(max = 100)
        String locationName,

        @Schema(description = "구역", example = "A")
        @NotBlank
        @Size(max = 10)
        String sector,

        @Schema(description = "지도 X 좌표", example = "123.4567")
        @NotNull
        BigDecimal mapX,

        @Schema(description = "지도 Y 좌표", example = "45.6789")
        @NotNull
        BigDecimal mapY,

        @Schema(description = "지도 표시 너비", example = "12.345", nullable = true)
        BigDecimal width,

        @Schema(description = "지도 표시 높이", example = "6.789", nullable = true)
        BigDecimal height,

        @Schema(description = "위치 타입", example = "STAGE", allowableValues = {"STAGE", "BOOTH", "ENTRANCE", "FACILITY", "OTHER"})
        @NotNull
        MapLocationType locationType,

        @Schema(description = "노출 순서. 생략 시 0으로 저장", example = "1", nullable = true)
        Integer displayOrder,

        @Schema(description = "노출 상태", example = "VISIBLE", allowableValues = {"VISIBLE", "HIDDEN"})
        @NotNull
        MapDisplayStatus displayStatus
) {
}
