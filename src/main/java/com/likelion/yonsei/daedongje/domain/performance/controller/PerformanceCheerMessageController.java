package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.common.response.PageResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceReviewResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceReviewSummaryResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.PerformanceCheerMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "공연 응원 메시지", description = "공연 응원 메시지 등록, 관리, 후기 수합 대시보드 API")
@RestController
@RequiredArgsConstructor
public class PerformanceCheerMessageController {

    private final PerformanceCheerMessageService cheerMessageService;

    @Operation(
            summary = "공연 응원 메시지 등록",
            description = "일반 사용자가 특정 공연에 응원 메시지를 등록합니다. 셋리스트 선택은 선택 사항이며, 로그인 없이 요청할 수 있습니다."
    )
    @PostMapping("/api/performances/{id}/cheer-messages")
    public ResponseEntity<ApiResponse<PerformanceCheerMessageResponse>> createCheerMessage(
            @PathVariable Long id,
            @Valid @RequestBody PerformanceCheerMessageCreateRequest request
    ) {
        PerformanceCheerMessageResponse response = cheerMessageService.createCheerMessage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "내 공연 응원 메시지 목록 조회",
            description = "공연팀 어드민이 본인 공연에 등록된 응원 메시지를 조회합니다. SUPER, PERFORMER 권한이 필요합니다."
    )
    @RequireAdminRole({AdminRole.PERFORMER, AdminRole.SUPER})
    @GetMapping("/api/admin/performances/me/cheer-messages")
    public ApiResponse<List<PerformanceCheerMessageResponse>> getMyPerformanceCheerMessages(
            @Parameter(hidden = true) @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(cheerMessageService.getMyPerformanceCheerMessages(currentAdmin));
    }

    @Operation(
            summary = "내 공연 후기 수합 요약 조회",
            description = "공연팀 어드민이 본인 공연의 가장 좋았던 무대 투표 결과를 조회합니다. 셋리스트가 선택된 응원 메시지만 투표 집계에 포함됩니다. SUPER, PERFORMER 권한이 필요합니다."
    )
    @RequireAdminRole({AdminRole.PERFORMER, AdminRole.SUPER})
    @GetMapping("/api/admin/performances/me/reviews/summary")
    public ApiResponse<PerformanceReviewSummaryResponse> getMyPerformanceReviewSummary(
            @Parameter(hidden = true) @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(cheerMessageService.getMyPerformanceReviewSummary(currentAdmin));
    }

    @Operation(
            summary = "내 공연 관객 후기 목록 조회",
            description = "공연팀 어드민이 본인 공연의 관객 후기 목록을 최신순으로 조회합니다. setlistId를 전달하면 특정 셋리스트에 대한 후기만 필터링할 수 있습니다. SUPER, PERFORMER 권한이 필요합니다."
    )
    @RequireAdminRole({AdminRole.PERFORMER, AdminRole.SUPER})
    @GetMapping("/api/admin/performances/me/reviews")
    public ApiResponse<PageResponse<PerformanceReviewResponse>> getMyPerformanceReviews(
            @Parameter(hidden = true) @CurrentAdmin AdminSessionUser currentAdmin,
            @Parameter(description = "페이지 번호입니다. 0부터 시작합니다.")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기입니다.")
            @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "셋리스트 ID입니다. 특정 곡 또는 무대에 대한 후기만 조회할 때 사용합니다.")
            @RequestParam(required = false) Long setlistId
    ) {
        // 잘못된 page/size(음수·0)는 PageRequest.of 에서 IllegalArgumentException → 500 이 되므로 방어한다.
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ApiResponse.success(cheerMessageService.getMyPerformanceReviews(currentAdmin, setlistId, pageable));
    }

    @Operation(
            summary = "내 공연 응원 메시지 숨김 처리",
            description = "공연팀 어드민이 본인 공연에 등록된 응원 메시지를 HIDDEN 상태로 변경합니다. SUPER, PERFORMER 권한이 필요합니다."
    )
    @RequireAdminRole({AdminRole.PERFORMER, AdminRole.SUPER})
    @DeleteMapping("/api/admin/performances/me/cheer-messages/{messageId}")
    public ResponseEntity<Void> deleteMyPerformanceCheerMessage(
            @Parameter(hidden = true) @CurrentAdmin AdminSessionUser currentAdmin,
            @PathVariable Long messageId
    ) {
        cheerMessageService.hideMyPerformanceCheerMessage(currentAdmin, messageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "공연 응원 메시지 전체 조회(운영진)",
            description = "운영진(SUPER, MASTER)이 전 공연의 응원 메시지를 전 상태(VISIBLE/HIDDEN) 기준으로 최신 등록 순으로 조회합니다. 부적절한 메시지를 공연에 관계없이 모더레이션하기 위한 운영용 조회입니다."
    )
    @RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER})
    @GetMapping("/api/admin/performances/cheer-messages")
    public ApiResponse<List<PerformanceCheerMessageResponse>> getAllCheerMessages() {
        return ApiResponse.success(cheerMessageService.getAllCheerMessages());
    }
}
