package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.BarrierFreeInfoResponse;
import com.likelion.yonsei.daedongje.domain.info.service.BarrierFreeInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "배리어프리 정보", description = "배리어프리 정보 조회 API")
@RestController
@RequestMapping("/api/barrier-free-infos")
@RequiredArgsConstructor
public class BarrierFreeInfoController {

    private final BarrierFreeInfoService barrierFreeInfoService;

    @Operation(summary = "배리어프리 정보 목록 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ApiResponse<List<BarrierFreeInfoResponse>> getBarrierFreeInfos(
            @Parameter(description = "시설 구분", example = "TOILET")
            @RequestParam(required = false, name = "facility_type") String facilityType
    ) {
        return ApiResponse.success(barrierFreeInfoService.getBarrierFreeInfos(facilityType));
    }

    @Operation(summary = "배리어프리 정보 단건 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 배리어프리 정보 (I-003)")
    @GetMapping("/{id}")
    public ApiResponse<BarrierFreeInfoResponse> getBarrierFreeInfo(@PathVariable Long id) {
        return ApiResponse.success(barrierFreeInfoService.getBarrierFreeInfo(id));
    }
}
