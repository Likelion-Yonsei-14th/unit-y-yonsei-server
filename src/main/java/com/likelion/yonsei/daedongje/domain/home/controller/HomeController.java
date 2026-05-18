package com.likelion.yonsei.daedongje.domain.home.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.home.dto.HomeBannerResponse;
import com.likelion.yonsei.daedongje.domain.home.dto.HomePopularBoothResponse;
import com.likelion.yonsei.daedongje.domain.home.service.HomeService;
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

    @Operation(summary = "오늘의 인기 부스 조회", description = "오늘 00시부터 현재까지 클릭 수가 많은 부스를 최대 5개까지 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/popular-booths")
    public ApiResponse<List<HomePopularBoothResponse>> getPopularBooths() {
        return ApiResponse.success(homeService.getPopularBooths());
    }
}
