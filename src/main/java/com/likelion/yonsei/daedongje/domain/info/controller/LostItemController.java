package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.service.LostItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "분실물", description = "분실물 관리 API")
@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

    private final LostItemService lostItemService;

    @Operation(summary = "분실물 목록 조회")
    @GetMapping
    public ApiResponse<List<LostItemResponse>> getLostItems() {
        return ApiResponse.success(lostItemService.getLostItems());
    }

    @Operation(summary = "분실물 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<LostItemResponse>> createLostItem(
            @Valid @RequestBody LostItemCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(lostItemService.createLostItem(request)));
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
    public ApiResponse<Void> deleteLostItem(@PathVariable Long lostItemId) {
        lostItemService.deleteLostItem(lostItemId);
        return ApiResponse.successEmpty();
    }
}
