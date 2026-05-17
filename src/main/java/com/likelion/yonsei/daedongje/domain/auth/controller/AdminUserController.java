package com.likelion.yonsei.daedongje.domain.auth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserDetailResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserListResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.service.AdminUserService;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin User", description = "어드민 계정 관리 API")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(
            summary = "어드민 계정 생성",
            description = "Super Admin이 부스, 공연, 총학생회 담당자의 어드민 계정을 생성합니다."
    )
    @RequireAdminRole(AdminRole.SUPER)
    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserCreateResponse>> createAdminUser(
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        AdminUserCreateResponse response = adminUserService.createAdminUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "어드민 계정 목록 조회",
            description = "Super Admin이 어드민 계정 목록을 조회합니다. role에 따른 필터링이 가능합니다."
    )
    @RequireAdminRole(AdminRole.SUPER)
    @GetMapping
    public ApiResponse<List<AdminUserListResponse>> getAdminUsers(
            @RequestParam(required = false) String role
    ) {
        return ApiResponse.success(adminUserService.getAdminUsers(role));
    }

    @Operation(
            summary = "어드민 계정 상세 조회",
            description = "Super Admin이 특정 어드민 계정의 상세 정보를 조회합니다."
    )
    @RequireAdminRole(AdminRole.SUPER)
    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailResponse> getAdminUserDetail(
            @PathVariable Long id
    ) {
        return ApiResponse.success(adminUserService.getAdminUserDetail(id));
    }

    @Operation(
            summary = "어드민 계정 삭제",
            description = "Super Admin이 MASTER, BOOTH, PERFORMER 어드민 계정을 삭제합니다. SUPER 계정은 삭제할 수 없습니다."
    )
    @RequireAdminRole(AdminRole.SUPER)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAdminUser(
            @PathVariable Long id
    ) {
        adminUserService.deleteAdminUser(id);
        return ApiResponse.successEmpty();
    }
}
