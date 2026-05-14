package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothStatusUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "부스 어드민", description = "부스 생성·수정·삭제 어드민 API")
@RestController
@RequestMapping("/api/admin/booths")
@RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH})
@RequiredArgsConstructor
public class BoothAdminController {

    private final BoothService boothService;

    @Operation(summary = "부스 생성", description = "어드민이 신규 부스를 등록한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패 / 운영 시간 오류 (B-003)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 부스 이름 (B-004)")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothResponse>> create(@RequestBody @Valid BoothCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(boothService.create(request)));
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

    @Operation(summary = "부스 운영 상태 변경", description = "부스 운영 여부를 OPEN / CLOSED / PREPARING 중 하나로 변경한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패 / 잘못된 status 값")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 담당 부스가 아닌 경우 (A-009)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @PatchMapping("/{id}/status")
    public ApiResponse<BoothResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid BoothStatusUpdateRequest request,
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(boothService.updateStatus(id, request.status(), currentAdmin));
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