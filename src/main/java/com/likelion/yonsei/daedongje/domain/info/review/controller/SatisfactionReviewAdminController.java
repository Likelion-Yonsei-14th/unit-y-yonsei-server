package com.likelion.yonsei.daedongje.domain.info.review.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewAdminResponse;
import com.likelion.yonsei.daedongje.domain.info.review.service.SatisfactionReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "만족도 리뷰 어드민", description = "만족도 리뷰 어드민 조회 API")
@RestController
@RequestMapping("/api/admin/info/reviews")
@RequireAdminRole(AdminRole.SUPER)
@RequiredArgsConstructor
public class SatisfactionReviewAdminController {

    private final SatisfactionReviewService satisfactionReviewService;

    @Operation(
            summary = "어드민 만족도 리뷰 조회",
            description = "SUPER 어드민이 만족도 리뷰 집계 정보와 페이지네이션된 리뷰 목록을 조회합니다."
    )
    @GetMapping
    public ApiResponse<SatisfactionReviewAdminResponse> getReviews(
            @Parameter(description = "페이지 번호. 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기. 최대 100", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(satisfactionReviewService.getAdminReviews(page, size));
    }
}