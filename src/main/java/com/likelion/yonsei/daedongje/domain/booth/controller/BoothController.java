package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booths")
public class BoothController {

    private final BoothService boothService;

    public BoothController(BoothService boothService) {
        this.boothService = boothService;
    }

    // 부스 생성
    @PostMapping
    public ResponseEntity<ApiResponse<BoothResponse>> create(@RequestBody @Valid BoothCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(boothService.create(request)));
    }

    // 부스 단건 조회
    @GetMapping("/{id}")
    public ApiResponse<BoothResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(boothService.getById(id));
    }

    // 부스 목록 조회 (필터: 날짜, 구역, 음식 여부)
    @GetMapping
    public ApiResponse<List<BoothResponse>> getList(
            @RequestParam(required = false) Integer date,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) Boolean isFood
    ) {
        return ApiResponse.success(boothService.getList(date, sector, isFood));
    }

    // 부스 수정
    @PutMapping("/{id}")
    public ApiResponse<BoothResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid BoothUpdateRequest request
    ) {
        return ApiResponse.success(boothService.update(id, request));
    }

    // 부스 삭제
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boothService.delete(id);
        return ApiResponse.successEmpty();
    }
}
