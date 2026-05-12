package com.likelion.yonsei.daedongje.domain.map.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.common.response.PageResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.map.dto.MapLocationResponse;
import com.likelion.yonsei.daedongje.domain.map.service.MapLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "지도 위치 어드민", description = "지도 위치 조회 어드민 API")
@RestController
@RequestMapping("/api/admin/map-locations")
@RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH, AdminRole.PERFORMER})
@RequiredArgsConstructor
public class MapLocationController {

    private final MapLocationService mapLocationService;

    @Operation(summary = "지도 위치 목록 조회", description = "관리자 페이지에서 지도 위치 목록을 필터와 페이지 조건으로 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이지 요청 값 오류")
    @GetMapping
    public ApiResponse<PageResponse<MapLocationResponse>> getList(
            @Parameter(description = "구역", example = "A")
            @RequestParam(required = false) String sector,
            @Parameter(description = "위치 타입", example = "STAGE")
            @RequestParam(name = "location_type", required = false) String locationType,
            @Parameter(description = "노출 상태", example = "VISIBLE")
            @RequestParam(name = "display_status", required = false) String displayStatus,
            @Parameter(description = "페이지 번호. 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기. 최대 100", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(mapLocationService.getList(sector, locationType, displayStatus, page, size));
    }

    @Operation(summary = "지도 위치 단건 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 지도 위치")
    @GetMapping("/{id}")
    public ApiResponse<MapLocationResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(mapLocationService.getById(id));
    }
}
