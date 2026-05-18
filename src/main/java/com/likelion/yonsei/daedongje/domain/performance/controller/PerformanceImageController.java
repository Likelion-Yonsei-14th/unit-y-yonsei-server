package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceImageResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "공연 이미지", description = "사용자 페이지에서 공연 이미지를 조회하는 API")
@RestController
@RequiredArgsConstructor
public class PerformanceImageController {

    private final PerformanceImageService performanceImageService;

    @Operation(
            summary = "공연 이미지 목록 조회",
            description = "특정 공연의 이미지 목록을 imageOrder ASC, id ASC 기준으로 조회합니다."
    )
    @GetMapping("/api/performances/{id}/images")
    public ApiResponse<List<PerformanceImageResponse>> getPerformanceImages(@PathVariable Long id) {
        return ApiResponse.success(performanceImageService.getPerformanceImages(id));
    }
}
