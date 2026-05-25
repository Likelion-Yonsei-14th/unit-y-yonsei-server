package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.festival.FestivalDayService;
import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.common.response.PageResponse;
import com.likelion.yonsei.daedongje.common.web.ClientIpResolver;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.ReservableBoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothClickLogService;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "부스", description = "부스 조회 API")
@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class BoothController {

    private final BoothService boothService;
    private final FestivalDayService festivalDayService;
    private final BoothClickLogService boothClickLogService;

    @Operation(summary = "현재 축제 일차 조회", description = "서버 현재 시간(KST) 기준으로 부스 필터에 적용할 축제 일차를 반환한다.\n\n- 2 = 2026년 5월 27일\n- 3 = 2026년 5월 28일\n- 4 = 2026년 5월 29일\n\n부스 운영 기간 이전이면 2, 이후면 4로 클램핑된다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/festival-day")
    public ApiResponse<Integer> getCurrentFestivalDay() {
        return ApiResponse.success(festivalDayService.getCurrentFestivalDay());
    }

    @Operation(summary = "예약 가능 부스 목록 조회", description = "예약 접수 중인 부스 목록과 각 부스의 현재 대기 팀 수를 반환한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/reservable")
    public ApiResponse<List<ReservableBoothResponse>> getReservableList() {
        return ApiResponse.success(boothService.getReservableList());
    }

    @Operation(summary = "부스 검색", description = "부스명 또는 단체명에 키워드가 포함된 부스를 검색한다. 대소문자를 구분하지 않는다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
    @GetMapping("/search")
    public ApiResponse<PageResponse<BoothResponse>> search(
            @Parameter(description = "검색 키워드 (부스명 또는 단체명)", example = "멋사")
            @RequestParam String keyword,
            @Parameter(description = "페이지 번호. 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(boothService.search(keyword, page, size));
    }

    @Operation(summary = "부스 단건 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @GetMapping("/{id}")
    public ApiResponse<BoothResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(boothService.getById(id));
    }

    @Operation(summary = "부스 클릭 로그 저장", description = "부스 상세 진입 이벤트를 저장한다. 동일 IP·부스 조합 기준 분당 10회로 요청을 제한한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 (B-001)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "클릭 로그 요청 한도 초과 (B-005)")
    @PostMapping("/{boothId}/clicks")
    public ApiResponse<Void> createClickLog(@PathVariable Long boothId, HttpServletRequest request) {
        boothClickLogService.create(boothId, ClientIpResolver.resolve(request));
        return ApiResponse.successEmpty();
    }

    @Operation(summary = "부스 목록 조회", description = "날짜·구역·음식 여부·푸드트럭 여부·운영상태를 AND 조건으로 필터링하며 page/size 로 페이지네이션한다. 필터를 생략해도 전체가 아니라 첫 페이지만 반환한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ApiResponse<PageResponse<BoothResponse>> getList(
            @Parameter(description = "축제 일차 (1=1일차, 2=2일차, 3=3일차, 4=4일차)", example = "1")
            @RequestParam(required = false) Integer date,
            @Parameter(description = "구역 (한글탑 / 백양로 / 송도)", example = "한글탑")
            @RequestParam(required = false) BoothSector sector,
            @Parameter(description = "음식 부스만 조회", example = "true")
            @RequestParam(required = false) Boolean isFood,
            @Parameter(description = "푸드트럭만 조회", example = "false")
            @RequestParam(required = false) Boolean isFoodTruck,
            @Parameter(description = "운영 상태 필터 (OPEN / CLOSED / PREPARING)", example = "OPEN")
            @RequestParam(required = false) BoothStatus status,
            @Parameter(description = "페이지 번호. 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(boothService.getList(date, sector, isFood, isFoodTruck, status, page, size));
    }
}
