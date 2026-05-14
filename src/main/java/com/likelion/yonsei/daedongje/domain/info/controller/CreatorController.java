package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorResponse;
import com.likelion.yonsei.daedongje.domain.info.service.CreatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "만든이들", description = "만든이들 조회 API")
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;

    @Operation(summary = "만든이들 목록 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ApiResponse<List<CreatorResponse>> getCreators() {
        return ApiResponse.success(creatorService.getCreators());
    }

    @Operation(summary = "만든이 단건 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 만든이 정보 (I-004)")
    @GetMapping("/{id}")
    public ApiResponse<CreatorResponse> getCreator(@PathVariable Long id) {
        return ApiResponse.success(creatorService.getCreator(id));
    }
}
