package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "부스", description = "부스 CRUD API")
@RestController
@RequestMapping("/api/booths")
public class BoothController {

    private final BoothService boothService;

    public BoothController(BoothService boothService) {
        this.boothService = boothService;
    }

    @Operation(summary = "부스 생성", description = "어드민이 신규 부스를 등록한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패 / 운영 시간 오류 (B-003)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 부스 이름 (B-004)")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothResponse>> create(@RequestBody @Valid BoothCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(boothService.create(request)));
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

    @Operation(summary = "부스 수정")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 부스 이름 (B-004)")
    @PutMapping("/{id}")
    public ApiResponse<BoothResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid BoothUpdateRequest request
    ) {
        return ApiResponse.success(boothService.update(id, request));
    }

    @Operation(summary = "부스 삭제")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boothService.delete(id);
        return ApiResponse.successEmpty();
    }
}
