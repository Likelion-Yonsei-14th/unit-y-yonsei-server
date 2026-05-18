package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceMyResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공연 어드민", description = "공연 어드민 본인 공연 정보 API")
@RestController
@RequestMapping("/api/admin/performances")
@RequireAdminRole({AdminRole.PERFORMER, AdminRole.SUPER})
@RequiredArgsConstructor
public class PerformanceAdminController {

    private final PerformanceService performanceService;

    @Operation(summary = "내 공연 정보 조회", description = "로그인한 공연 어드민 계정에 연결된 공연 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<PerformanceMyResponse> getMyPerformance(
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(performanceService.getMyPerformance(currentAdmin));
    }

    @Operation(summary = "내 공연 정보 수정", description = "로그인한 공연 어드민 계정에 연결된 공연 기본 정보를 수정합니다.")
    @PatchMapping("/me")
    public ApiResponse<PerformanceMyResponse> updateMyPerformance(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @Valid @RequestBody PerformanceUpdateRequest request
    ) {
        return ApiResponse.success(performanceService.updateMyPerformance(currentAdmin, request));
    }

    @Operation(summary = "내 공연 정보 삭제", description = "로그인한 공연 어드민 계정에 연결된 공연 정보를 삭제합니다.")
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyPerformance(
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        performanceService.deleteMyPerformance(currentAdmin);
        return ApiResponse.successEmpty();
    }
}
