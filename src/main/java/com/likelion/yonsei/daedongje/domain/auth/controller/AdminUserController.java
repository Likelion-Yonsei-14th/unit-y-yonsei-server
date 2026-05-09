package com.likelion.yonsei.daedongje.domain.auth.controller;

import com.likelion.yonsei.daedongje.common.auth.RequireAdminRole;
import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateRequest;
import com.likelion.yonsei.daedongje.domain.auth.dto.AdminUserCreateResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserCreateResponse>> createAdminUser(
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        AdminUserCreateResponse response = adminUserService.createAdminUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
