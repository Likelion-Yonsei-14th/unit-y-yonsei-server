package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceImageCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceImageResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공연 이미지 어드민", description = "공연 어드민이 본인 공연의 이미지를 등록하고 삭제하는 API")
@RestController
@RequiredArgsConstructor
public class PerformanceImageAdminController {

    private final PerformanceImageService performanceImageService;

    @Operation(
            summary = "공연 이미지 등록",
            description = "로그인한 공연 어드민 계정에 연결된 공연에 업로드 완료된 이미지 URL을 등록합니다."
    )
    @RequireAdminRole(AdminRole.PERFORMER)
    @PostMapping("/api/admin/performances/me/images")
    public ResponseEntity<ApiResponse<PerformanceImageResponse>> createMyPerformanceImage(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @Valid @RequestBody PerformanceImageCreateRequest request
    ) {
        PerformanceImageResponse response =
                performanceImageService.createMyPerformanceImage(currentAdmin, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "공연 이미지 삭제",
            description = "로그인한 공연 어드민이 본인 공연에 연결된 이미지만 삭제합니다."
    )
    @RequireAdminRole(AdminRole.PERFORMER)
    @DeleteMapping("/api/admin/performances/me/images/{imageId}")
    public ResponseEntity<Void> deleteMyPerformanceImage(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @PathVariable Long imageId
    ) {
        performanceImageService.deleteMyPerformanceImage(currentAdmin, imageId);
        return ResponseEntity.noContent().build();
    }
}
