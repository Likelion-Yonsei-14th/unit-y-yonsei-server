package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.service.LostItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Lost Item Admin", description = "분실물 관리자 API")
@RestController
@RequestMapping("/api/admin/lost-items")
@RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH})
@RequiredArgsConstructor
public class LostItemAdminController {

    private final LostItemService lostItemService;

    @Operation(summary = "분실물 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LostItemResponse> createLostItem(
            @Valid @RequestBody LostItemCreateRequest request
    ) {
        return ApiResponse.success(lostItemService.createLostItem(request));
    }

    @Operation(summary = "분실물 수정")
    @PutMapping("/{lostItemId}")
    public ApiResponse<LostItemResponse> updateLostItem(
            @PathVariable Long lostItemId,
            @Valid @RequestBody LostItemUpdateRequest request
    ) {
        return ApiResponse.success(lostItemService.updateLostItem(lostItemId, request));
    }

    @Operation(summary = "분실물 삭제")
    @DeleteMapping("/{lostItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteLostItem(@PathVariable Long lostItemId) {
        lostItemService.deleteLostItem(lostItemId);
        return ApiResponse.successEmpty();
    }
}
