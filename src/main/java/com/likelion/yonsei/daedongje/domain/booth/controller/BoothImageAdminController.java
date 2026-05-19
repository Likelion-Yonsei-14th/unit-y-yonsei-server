package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothImageCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothImageResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothImageUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "부스 이미지 관리자 API", description = "부스 이미지 생성, 수정, 삭제 API")
@RestController
@RequestMapping("/api/admin/booths/{boothId}/images")
@RequireAdminRole({ AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH })
@RequiredArgsConstructor
public class BoothImageAdminController {

    private final BoothImageService boothImageService;

    @Operation(summary = "부스 이미지 생성", description = "특정 부스에 이미지를 생성합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 / 담당 부스 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "부스 이미지 표시 순서 중복")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothImageResponse>> create(
            @CurrentAdmin AdminSessionUser admin,
            @PathVariable Long boothId,
            @Valid @RequestBody BoothImageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(boothImageService.create(admin, boothId, request)));
    }

    @Operation(summary = "부스 이미지 수정", description = "부스 이미지 정보를 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 / 담당 부스 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 또는 부스 이미지")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "부스 이미지 표시 순서 중복")
    @PatchMapping("/{imageId}")
    public ApiResponse<BoothImageResponse> update(
            @CurrentAdmin AdminSessionUser admin,
            @PathVariable Long boothId,
            @PathVariable Long imageId,
            @Valid @RequestBody BoothImageUpdateRequest request) {
        return ApiResponse.success(boothImageService.update(admin, boothId, imageId, request));
    }

    @Operation(summary = "부스 이미지 삭제", description = "부스 이미지를 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 / 담당 부스 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 또는 부스 이미지")
    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> delete(
            @CurrentAdmin AdminSessionUser admin,
            @PathVariable Long boothId,
            @PathVariable Long imageId) {
        boothImageService.delete(admin, boothId, imageId);
        return ApiResponse.successEmpty();
    }
}
