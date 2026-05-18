package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceSetlistCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceSetlistResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceSetlistUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceSetlistService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공연 셋리스트 어드민", description = "공연 어드민이 본인 공연의 셋리스트를 등록, 수정, 삭제하는 API")
@RestController
@RequiredArgsConstructor
public class PerformanceSetlistAdminController {

    private final PerformanceSetlistService performanceSetlistService;

    @Operation(
            summary = "공연 셋리스트 등록",
            description = "로그인한 공연 어드민 계정에 연결된 공연에 셋리스트를 등록합니다."
    )
    @RequireAdminRole(AdminRole.PERFORMER)
    @PostMapping("/api/admin/performances/me/setlists")
    public ResponseEntity<ApiResponse<PerformanceSetlistResponse>> createMyPerformanceSetlist(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @Valid @RequestBody PerformanceSetlistCreateRequest request
    ) {
        PerformanceSetlistResponse response =
                performanceSetlistService.createMyPerformanceSetlist(currentAdmin, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "공연 셋리스트 수정",
            description = "로그인한 공연 어드민이 본인 공연에 연결된 셋리스트만 수정합니다."
    )
    @RequireAdminRole(AdminRole.PERFORMER)
    @PatchMapping("/api/admin/performances/me/setlists/{setlistId}")
    public ApiResponse<PerformanceSetlistResponse> updateMyPerformanceSetlist(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @PathVariable Long setlistId,
            @Valid @RequestBody PerformanceSetlistUpdateRequest request
    ) {
        return ApiResponse.success(
                performanceSetlistService.updateMyPerformanceSetlist(currentAdmin, setlistId, request)
        );
    }

    @Operation(
            summary = "공연 셋리스트 삭제",
            description = "로그인한 공연 어드민이 본인 공연에 연결된 셋리스트만 삭제합니다."
    )
    @RequireAdminRole(AdminRole.PERFORMER)
    @DeleteMapping("/api/admin/performances/me/setlists/{setlistId}")
    public ResponseEntity<Void> deleteMyPerformanceSetlist(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @PathVariable Long setlistId
    ) {
        performanceSetlistService.deleteMyPerformanceSetlist(currentAdmin, setlistId);
        return ResponseEntity.noContent().build();
    }
}
