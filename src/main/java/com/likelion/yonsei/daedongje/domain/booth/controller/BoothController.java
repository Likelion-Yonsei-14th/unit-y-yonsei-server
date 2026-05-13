package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.ReservableBoothResponse;
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

    @Operation(summary = "예약 가능 부스 목록 조회", description = "예약 접수 중인 부스 목록과 각 부스의 현재 대기 팀 수를 반환한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/reservable")
    public ApiResponse<List<ReservableBoothResponse>> getReservableList() {
        return ApiResponse.success(boothService.getReservableList());
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
