package com.likelion.yonsei.daedongje.domain.reservation.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCancelRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationResponse;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "예약 어드민", description = "예약 목록 조회·입장 처리·취소 어드민 API")
@RestController
@RequestMapping("/api/admin/reservations")
@RequireAdminRole({AdminRole.SUPER, AdminRole.BOOTH})
@RequiredArgsConstructor
public class ReservationAdminController {

    private final ReservationService reservationService;

    @Operation(summary = "부스별 예약 목록 조회", description = "status 파라미터로 대기·완료·취소 탭 필터링. 생략 시 전체 조회.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/booths/{boothId}")
    public ApiResponse<List<ReservationResponse>> getListByBooth(
            @PathVariable Long boothId,
            @Parameter(description = "예약 상태 필터 (PENDING / CONFIRMED / CANCELLED). 생략 시 전체 조회", example = "PENDING")
            @RequestParam(required = false) ReservationStatus status
    ) {
        return ApiResponse.success(reservationService.getListByBooth(boothId, status));
    }

    @Operation(summary = "예약 입장 처리", description = "예약자가 현장에 도착해 입장할 때 CONFIRMED 처리한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입장 처리 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "취소된 예약 (R-004)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 예약 (R-001)")
    @PatchMapping("/{id}/confirm")
    public ApiResponse<ReservationResponse> confirm(@PathVariable Long id) {
        return ApiResponse.success(reservationService.confirm(id));
    }

    @Operation(summary = "예약 취소")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 취소된 예약 (R-003)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 예약 (R-001)")
    @PatchMapping("/{id}/cancel")
    public ApiResponse<ReservationResponse> cancel(
            @PathVariable Long id,
            @RequestBody ReservationCancelRequest request
    ) {
        return ApiResponse.success(reservationService.cancel(id, request));
    }
}
