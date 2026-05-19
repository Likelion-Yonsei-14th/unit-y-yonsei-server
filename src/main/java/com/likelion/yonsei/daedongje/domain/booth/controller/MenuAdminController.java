package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuReorderRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 / 담당 부스 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "메뉴 표시 순서 중복")
    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(
            @CurrentAdmin AdminSessionUser admin,
            @PathVariable Long boothId,
            @Valid @RequestBody MenuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(menuService.create(admin, boothId, request)));
    }

    @Operation(summary = "메뉴 수정", description = "메뉴 정보를 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 / 담당 부스 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 또는 메뉴")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "메뉴 표시 순서 중복")
    @PatchMapping("/{menuId}")
    public ApiResponse<MenuResponse> update(
            @CurrentAdmin AdminSessionUser admin,
            @PathVariable Long boothId,
            @PathVariable Long menuId,
            @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.success(menuService.update(admin, boothId, menuId, request));
    }

    @Operation(summary = "메뉴 순서 재정렬", description = "부스 메뉴의 표시 순서를 요청한 순서대로 일괄 재정렬합니다. menuIds는 부스 메뉴 ID의 완전한 순열이어야 합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재정렬 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패 / 부스 메뉴 구성과 불일치")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 / 담당 부스 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스")
    @PutMapping("/order")
    public ApiResponse<List<MenuResponse>> reorder(
            @CurrentAdmin AdminSessionUser admin,
            @PathVariable Long boothId,
            @Valid @RequestBody MenuReorderRequest request) {
        return ApiResponse.success(menuService.reorder(admin, boothId, request.menuIds()));
    }

    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 / 담당 부스 아님")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 부스 또는 메뉴")
    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> delete(
            @CurrentAdmin AdminSessionUser admin,
            @PathVariable Long boothId,
            @PathVariable Long menuId) {
        menuService.delete(admin, boothId, menuId);
        return ApiResponse.successEmpty();
    }
}
