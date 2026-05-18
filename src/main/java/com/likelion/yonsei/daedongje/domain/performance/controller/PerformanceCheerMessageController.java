package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceCheerMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "공연 응원 메시지", description = "공연 응원 메시지 등록, 조회, 관리자 조회 및 삭제 API")
@RestController
@RequiredArgsConstructor
public class PerformanceCheerMessageController {

    private final PerformanceCheerMessageService cheerMessageService;

    @Operation(summary = "공연 응원 메시지 등록", description = "사용자가 공연과 선택 셋리스트에 응원 메시지를 등록합니다.")
    @PostMapping("/performances/{id}/cheer-messages")
    public ResponseEntity<ApiResponse<PerformanceCheerMessageResponse>> createCheerMessage(
            @PathVariable Long id,
            @Valid @RequestBody PerformanceCheerMessageCreateRequest request
    ) {
        PerformanceCheerMessageResponse response = cheerMessageService.createCheerMessage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "공연별 응원 메시지 목록 조회", description = "공연별 VISIBLE 응원 메시지를 createdAt ASC, id ASC 기준으로 조회합니다.")
    @GetMapping("/performances/{id}/cheer-messages")
    public ApiResponse<List<PerformanceCheerMessageResponse>> getCheerMessages(@PathVariable Long id) {
        return ApiResponse.success(cheerMessageService.getVisibleCheerMessages(id));
    }

    @Operation(summary = "내 공연 응원 메시지 목록 조회", description = "공연 어드민이 본인 공연의 응원 메시지를 전체 상태 기준으로 조회합니다.")
    @RequireAdminRole({AdminRole.PERFORMER, AdminRole.SUPER})
    @GetMapping("/api/admin/performances/me/cheer-messages")
    public ApiResponse<List<PerformanceCheerMessageResponse>> getMyPerformanceCheerMessages(
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(cheerMessageService.getMyPerformanceCheerMessages(currentAdmin));
    }

    @Operation(summary = "내 공연 응원 메시지 삭제", description = "공연 어드민이 본인 공연의 응원 메시지를 HIDDEN 상태로 변경합니다.")
    @RequireAdminRole({AdminRole.PERFORMER, AdminRole.SUPER})
    @DeleteMapping("/api/admin/performances/me/cheer-messages/{messageId}")
    public ResponseEntity<Void> deleteMyPerformanceCheerMessage(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @PathVariable Long messageId
    ) {
        cheerMessageService.hideMyPerformanceCheerMessage(currentAdmin, messageId);
        return ResponseEntity.noContent().build();
    }
}
