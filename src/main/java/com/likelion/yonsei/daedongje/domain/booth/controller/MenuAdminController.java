package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메뉴 관리자 API", description = "메뉴 생성, 수정, 삭제 API")
@RestController
@RequestMapping("/api/admin/booths/{boothId}/menus")
@RequireAdminRole({ AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH })
@RequiredArgsConstructor
public class MenuAdminController {

    private final MenuService menuService;

    @Operation(summary = "메뉴 생성", description = "특정 부스에 메뉴를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(
            @PathVariable Long boothId,
            @Valid @RequestBody MenuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(menuService.create(boothId, request)));
    }

    @Operation(summary = "메뉴 수정", description = "메뉴 정보를 수정합니다.")
    @PutMapping("/{menuId}")
    public ApiResponse<MenuResponse> update(
            @PathVariable Long boothId,
            @PathVariable Long menuId,
            @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.success(menuService.update(boothId, menuId, request));
    }

    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다.")
    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> delete(
            @PathVariable Long boothId,
            @PathVariable Long menuId) {
        menuService.delete(boothId, menuId);
        return ApiResponse.successEmpty();
    }
}
