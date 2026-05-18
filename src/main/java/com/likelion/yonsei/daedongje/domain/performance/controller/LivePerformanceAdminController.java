package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.performance.dto.LivePerformanceUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.LivePerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공연 라이브 어드민", description = "운영진(SUPER)이 라이브 공연을 지정/교체/해제하는 API")
@RestController
@RequestMapping("/api/admin/performances")
@RequireAdminRole(AdminRole.SUPER)
@RequiredArgsConstructor
public class LivePerformanceAdminController {

    private final LivePerformanceService livePerformanceService;

    @Operation(
            summary = "라이브 공연 지정/해제",
            description = "현재 라이브 공연을 지정·교체하거나, performanceId를 null로 보내 해제합니다. 해제 시 data는 null입니다."
    )
    @PutMapping("/live")
    public ApiResponse<PerformanceCurrentResponse> updateLivePerformance(
            @RequestBody LivePerformanceUpdateRequest request
    ) {
        return ApiResponse.success(livePerformanceService.updateLivePerformance(request.performanceId()));
    }
}
