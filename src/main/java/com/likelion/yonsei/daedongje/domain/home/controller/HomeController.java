package com.likelion.yonsei.daedongje.domain.home.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.home.dto.HomeBannerResponse;
import com.likelion.yonsei.daedongje.domain.home.service.HomeService;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "홈", description = "홈 화면 API")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "메인 배너 목록 조회", description = "홈 화면에 노출할 메인 홍보 배너 목록을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/banners")
    public ApiResponse<List<HomeBannerResponse>> getBanners() {
        return ApiResponse.success(homeService.getBanners());
    }

    @Operation(summary = "현재 진행 중인 공연 조회", description = "홈 화면에 노출할 현재 진행 중인 공연 정보를 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "현재 진행 중인 공연 없음 (P-006)")
    @GetMapping("/current-performance")
    public ApiResponse<PerformanceCurrentResponse> getCurrentPerformance() {
        return ApiResponse.success(homeService.getCurrentPerformance());
    }
}
