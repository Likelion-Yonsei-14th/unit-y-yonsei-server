package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "부스", description = "부스 조회 API")
@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class BoothController {

    private final BoothService boothService;

    @Operation(summary = "현재 축제 일차 조회", description = "서버 현재 시간(KST) 기준으로 부스 필터에 적용할 축제 일차를 반환한다.\n\n- 2 = 5월 27일\n- 3 = 5월 28일\n- 4 = 5월 29일\n\n부스 운영 기간 이전이면 2, 이후면 4로 클램핑된다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/festival-day")
    public ApiResponse<Integer> getCurrentFestivalDay() {
        return ApiResponse.success(boothService.getCurrentFestivalDay());
    }

    @Operation(summary = "부스 단건 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @GetMapping("/{id}")
    public ApiResponse<BoothResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(boothService.getById(id));
    }

    @Operation(summary = "부스 목록 조회", description = "날짜·구역·음식 여부를 AND 조건으로 필터링한다. 파라미터를 생략하면 전체 조회.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ApiResponse<List<BoothResponse>> getList(
            @Parameter(description = "축제 일차 (1=1일차, 2=2일차, 3=3일차, 4=4일차)", example = "1")
            @RequestParam(required = false) Integer date,
            @Parameter(description = "구역 (한글탑 / 백양로 / 송도)", example = "한글탑")
            @RequestParam(required = false) BoothSector sector,
            @Parameter(description = "음식 부스만 조회", example = "true")
            @RequestParam(required = false) Boolean isFood
    ) {
        return ApiResponse.success(boothService.getList(date, sector, isFood));
    }
}
