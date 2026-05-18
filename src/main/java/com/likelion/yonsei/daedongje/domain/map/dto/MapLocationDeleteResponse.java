package com.likelion.yonsei.daedongje.domain.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "지도 위치 삭제 응답")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapLocationDeleteResponse {

    @Schema(description = "삭제된 지도 위치 ID", example = "1")
    private Long id;

    @Schema(description = "삭제 성공 여부", example = "true")
    private Boolean deleted;

    public static MapLocationDeleteResponse of(Long id) {
        return new MapLocationDeleteResponse(id, true);
    }
}
