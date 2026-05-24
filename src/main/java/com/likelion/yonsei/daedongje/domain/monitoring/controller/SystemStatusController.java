package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import com.likelion.yonsei.daedongje.domain.monitoring.service.RecentErrorLogService;
import com.likelion.yonsei.daedongje.domain.monitoring.service.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 "시스템 상태" 페이지 백엔드 API. SUPER/MASTER만 접근.
 * 라이브 헬스·최근 ERROR 로그·현재 활성 알림을 인-프로세스/Redis에서 읽어 내려준다.
 */
@Tag(name = "시스템 상태 어드민", description = "서버 라이브 상태·최근 에러·활성 알림 조회 (관리자)")
@RestController
@RequestMapping("/api/admin/system")
@RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER})
public class SystemStatusController {

    private final SystemHealthService systemHealthService;
    private final RecentErrorLogService recentErrorLogService;
    private final ActiveAlertStore activeAlertStore;

    public SystemStatusController(SystemHealthService systemHealthService,
                                  RecentErrorLogService recentErrorLogService,
                                  ActiveAlertStore activeAlertStore) {
        this.systemHealthService = systemHealthService;
        this.recentErrorLogService = recentErrorLogService;
        this.activeAlertStore = activeAlertStore;
    }

    @Operation(summary = "서버 라이브 상태 스냅샷")
    @GetMapping("/health")
    public ApiResponse<SystemHealthResponse> health() {
        return ApiResponse.success(systemHealthService.snapshot());
    }

    @Operation(summary = "최근 ERROR 로그")
    @GetMapping("/errors")
    public ApiResponse<List<ErrorLogEntry>> errors() {
        return ApiResponse.success(recentErrorLogService.recent());
    }

    @Operation(summary = "현재 발생 중인 알림")
    @GetMapping("/alerts")
    public ApiResponse<List<ActiveAlertResponse>> alerts() {
        return ApiResponse.success(activeAlertStore.findAllActive());
    }
}
