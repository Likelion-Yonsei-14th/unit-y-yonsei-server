package com.likelion.yonsei.daedongje.domain.reservation.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationAdminStatusRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationSummaryResponse;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "예약 어드민", description = "예약 목록 조회·입장 처리·취소 어드민 API")
@RestController
@RequestMapping("/api/admin/reservations")
@RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH})
@RequiredArgsConstructor
public class ReservationAdminController {

    private final ReservationService reservationService;

    @Operation(
            summary = "부스별 예약 목록 조회",
            description = """
                    status 파라미터로 대기·완료·취소 탭 필터링. 생략 시 전체 조회.
                    - `SUPER`: 모든 부스의 예약 조회 가능.
                    - `BOOTH`: 본인이 담당하는 부스의 예약만 조회 가능. 타 부스 접근 시 403 반환.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "담당 부스가 아닌 경우 (BOOTH 권한)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @GetMapping("/booths/{boothId}")
    public ApiResponse<List<ReservationResponse>> getListByBooth(
            @PathVariable Long boothId,
            @Parameter(description = "예약 상태 필터 (PENDING / CONFIRMED / CANCELLED). 생략 시 전체 조회", example = "PENDING")
            @RequestParam(required = false) ReservationStatus status,
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(reservationService.getListByBooth(boothId, status, currentAdmin));
    }

    @Operation(
            summary = "부스별 예약 현황 요약 조회",
            description = """
                    부스별 예약 상태 카운트(대기·완료·취소)와 조회 범위 전체의 합산을 반환한다.
                    - `SUPER`, `MASTER`: 모든 부스의 현황 조회.
                    - `BOOTH`: 본인이 담당하는 부스의 현황만 조회.
                    예약이 0건인 부스는 응답 `booths`에서 생략된다. 빈 결과도 200으로 응답한다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/summary")
    public ApiResponse<ReservationSummaryResponse> getSummary(
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(reservationService.getSummary(currentAdmin));
    }

    @Operation(
            summary = "예약 단건 조회",
            description = """
                    - `SUPER`: 모든 예약 조회 가능.
                    - `BOOTH`: 본인 담당 부스의 예약만 조회 가능. 타 부스 예약 접근 시 403 반환.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "담당 부스가 아닌 경우 (BOOTH 권한)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 예약 (R-001)")
    @GetMapping("/{id}")
    public ApiResponse<ReservationResponse> getById(
            @PathVariable Long id,
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(reservationService.getByIdForAdmin(id, currentAdmin));
    }

    @Operation(
            summary = "예약 상태 변경",
            description = """
                    예약 상태를 변경한다.
                    - `CONFIRMED`: 예약자가 현장에 도착해 입장 처리. 이미 취소된 예약에는 적용 불가 (R-004).
                    - `CANCELLED`: 예약 취소. 이미 취소된 예약에는 적용 불가 (R-003).
                    - `BOOTH` 권한은 본인 담당 부스의 예약만 변경 가능. 타 부스 접근 시 403 반환.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 상태 변경 (R-003, R-004, R-005)",
            content = @Content(schema = @Schema(example = "{\"success\":false,\"error\":{\"code\":\"R-003\",\"message\":\"이미 취소된 예약입니다.\"}}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "담당 부스가 아닌 경우 (BOOTH 권한)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 예약 (R-001)")
    @PatchMapping("/{id}/status")
    public ApiResponse<ReservationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid ReservationAdminStatusRequest request,
            @CurrentAdmin AdminSessionUser currentAdmin
    ) {
        return ApiResponse.success(reservationService.updateStatusByAdmin(id, request, currentAdmin));
    }
}
