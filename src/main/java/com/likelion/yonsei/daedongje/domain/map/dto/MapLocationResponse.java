package com.likelion.yonsei.daedongje.domain.map.dto;

import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "지도 위치 응답")
@Getter
@Builder
public class MapLocationResponse {

    @Schema(description = "지도 위치 ID", example = "1")
    private Long id;

    @Schema(description = "지도 위치 이름", example = "백양로 메인 스테이지")
    private String locationName;

    @Schema(description = "구역", example = "A")
    private String sector;

    @Schema(description = "지도 X 좌표", example = "123.4567")
    private BigDecimal mapX;

    @Schema(description = "지도 Y 좌표", example = "45.6789")
    private BigDecimal mapY;

    @Schema(description = "지도 표시 너비", example = "12.345")
    private BigDecimal width;

    @Schema(description = "지도 표시 높이", example = "6.789")
    private BigDecimal height;

    @Schema(description = "위치 타입", example = "STAGE")
    private String locationType;

    @Schema(description = "노출 순서", example = "1")
    private Integer displayOrder;

    @Schema(description = "노출 상태", example = "VISIBLE")
    private String displayStatus;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    public static MapLocationResponse from(MapLocation mapLocation) {
        return MapLocationResponse.builder()
                .id(mapLocation.getId())
                .locationName(mapLocation.getLocationName())
                .sector(mapLocation.getSector())
                .mapX(mapLocation.getMapX())
                .mapY(mapLocation.getMapY())
                .width(mapLocation.getWidth())
                .height(mapLocation.getHeight())
                .locationType(mapLocation.getLocationType())
                .displayOrder(mapLocation.getDisplayOrder())
                .displayStatus(mapLocation.getDisplayStatus())
                .createdAt(mapLocation.getCreatedAt())
                .updatedAt(mapLocation.getUpdatedAt())
                .build();
    }
}
