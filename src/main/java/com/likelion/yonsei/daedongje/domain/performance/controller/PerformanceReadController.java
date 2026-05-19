package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceDetailResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceListResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceTimetableResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.LivePerformanceService;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "공연", description = "사용자용 공연 조회 API")
@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceReadController {

    private final PerformanceReadService performanceReadService;
    private final LivePerformanceService livePerformanceService;

    @Operation(summary = "공연 목록 조회", description = "사용자에게 노출 가능한 공연 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<PerformanceListResponse>> getPerformances() {
        return ApiResponse.success(performanceReadService.getPerformances());
    }

    @Operation(summary = "현재 공연 조회", description = "현재 진행 중인 공연 정보를 조회합니다.")
    @GetMapping("/current")
    public ApiResponse<PerformanceCurrentResponse> getCurrentPerformance() {
        return ApiResponse.success(performanceReadService.getCurrentPerformance());
    }

    @Operation(summary = "공연 타임테이블 조회", description = "공연 타임테이블을 공연 일차와 시작 시간 순으로 조회합니다.")
    @GetMapping("/timetable")
    public ApiResponse<List<PerformanceTimetableResponse>> getPerformanceTimetable() {
        return ApiResponse.success(performanceReadService.getPerformanceTimetable());
    }

    @Operation(summary = "라이브 공연 조회", description = "운영진이 수동 지정한 현재 라이브 공연을 조회합니다. 미지정 시 data는 null입니다.")
    @GetMapping("/live")
    public ApiResponse<PerformanceCurrentResponse> getLivePerformance() {
        return ApiResponse.success(livePerformanceService.getLivePerformance());
    }

    @Operation(summary = "공연 상세 조회", description = "사용자에게 노출 가능한 공연 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<PerformanceDetailResponse> getPerformanceDetail(
            @PathVariable Long id
    ) {
        return ApiResponse.success(performanceReadService.getPerformanceDetail(id));
    }
}
