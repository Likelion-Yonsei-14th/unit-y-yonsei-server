package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceSetlistResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceSetlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "공연 셋리스트", description = "사용자 페이지에서 공연 셋리스트를 조회하는 API")
@RestController
@RequiredArgsConstructor
public class PerformanceSetlistController {

    private final PerformanceSetlistService performanceSetlistService;

    @Operation(
            summary = "공연 셋리스트 목록 조회",
            description = "특정 공연의 셋리스트 목록을 songOrder ASC, id ASC 기준으로 조회합니다."
    )
    @GetMapping("/api/performances/{id}/setlists")
    public ApiResponse<List<PerformanceSetlistResponse>> getPerformanceSetlists(@PathVariable Long id) {
        return ApiResponse.success(performanceSetlistService.getPerformanceSetlists(id));
    }
}
