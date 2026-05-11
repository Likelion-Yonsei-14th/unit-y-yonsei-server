package com.likelion.yonsei.daedongje.domain.reservation.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationMyRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationUserCancelRequest;
import com.likelion.yonsei.daedongje.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "예약", description = "예약 생성·조회 API")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "예약 생성", description = "특정 부스에 예약을 생성한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패 / 예약 불가 부스 (R-002)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @PostMapping("/booths/{boothId}")
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            @PathVariable Long boothId,
            @RequestBody @Valid ReservationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reservationService.create(boothId, request)));
    }

    @Operation(
            summary = "내 예약 목록 조회",
            description = """
                    이름·연락처·PIN으로 본인의 예약 내역을 조회한다.

                    **왜 POST인가?**
                    전화번호·PIN은 민감한 개인 정보다. GET 방식으로 쿼리 파라미터에 실으면
                    서버 액세스 로그, 브라우저 히스토리, Referer 헤더에 평문으로 노출될 수 있다.
                    이를 방지하기 위해 POST + Request Body 방식을 사용한다.

                    - PIN을 설정하지 않고 예약한 경우 `pin` 생략 가능.
                    - `status`로 상태 필터링 가능 (생략 시 전체 조회).
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @PostMapping("/my")
    public ApiResponse<List<ReservationResponse>> getMyList(
            @RequestBody @Valid ReservationMyRequest request
    ) {
        return ApiResponse.success(
                reservationService.getListByBooker(
                        request.bookerName(), request.phoneNumber(), request.pin(), request.status()));
    }

    @Operation(
            summary = "사용자 예약 취소",
            description = """
                    예약자 본인이 이름·연락처·PIN으로 소유권을 확인한 뒤 예약을 취소한다.
                    - PIN을 설정한 예약은 PIN이 일치해야 취소 가능.
                    - 소유권 불일치 시 보안상 존재하지 않는 예약과 동일한 404 반환.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 취소된 예약 (R-003)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 예약 또는 소유권 불일치 (R-001)")
    @PatchMapping("/{id}/status")
    public ApiResponse<ReservationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid ReservationUserCancelRequest request
    ) {
        return ApiResponse.success(reservationService.cancelByBooker(id, request));
    }
}
