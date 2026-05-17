package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemResponse;
import com.likelion.yonsei.daedongje.domain.info.service.LostItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Lost Item", description = "분실물 조회 API")
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
}
