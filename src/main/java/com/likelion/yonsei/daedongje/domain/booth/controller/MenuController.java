package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuResponse;
import com.likelion.yonsei.daedongje.domain.booth.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메뉴 API", description = "메뉴 조회 API")
@RestController
@RequestMapping("/api/booths/{boothId}/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "메뉴 목록 조회", description = "특정 부스의 메뉴 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<MenuResponse>> getMenus(
            @PathVariable Long boothId) {
        return ApiResponse.success(menuService.getListByBooth(boothId));
    }

    @Operation(summary = "메뉴 단건 조회", description = "특정 메뉴를 조회합니다.")
    @GetMapping("/{menuId}")
    public ApiResponse<MenuResponse> getMenu(
            @PathVariable Long boothId,
            @PathVariable Long menuId) {
        return ApiResponse.success(menuService.getById(boothId, menuId));
    }
}
