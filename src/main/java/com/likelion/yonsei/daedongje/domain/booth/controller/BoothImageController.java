package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothImageResponse;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "부스 이미지 API", description = "부스 이미지 조회 API")
@RestController
@RequestMapping("/api/booths/{boothId}/images")
@RequiredArgsConstructor
public class BoothImageController {

    private final BoothImageService boothImageService;

    @Operation(summary = "부스 이미지 목록 조회", description = "특정 부스의 이미지 목록을 표시 순서 기준으로 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스")
    @GetMapping
    public ApiResponse<List<BoothImageResponse>> getBoothImages(@PathVariable Long boothId) {
        return ApiResponse.success(boothImageService.getListByBooth(boothId));
    }

    @Operation(summary = "부스 이미지 단건 조회", description = "특정 부스 이미지를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 또는 부스 이미지")
    @GetMapping("/{imageId}")
    public ApiResponse<BoothImageResponse> getBoothImage(
            @PathVariable Long boothId,
            @PathVariable Long imageId) {
        return ApiResponse.success(boothImageService.getById(boothId, imageId));
    }
}
