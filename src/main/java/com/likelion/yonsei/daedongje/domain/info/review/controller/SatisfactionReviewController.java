package com.likelion.yonsei.daedongje.domain.info.review.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateResponse;
import com.likelion.yonsei.daedongje.domain.info.review.service.SatisfactionReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "만족도 리뷰", description = "만족도 평가 및 리뷰 제출 API")
@RestController
@RequiredArgsConstructor
public class SatisfactionReviewController {

    private final SatisfactionReviewService satisfactionReviewService;

    @Operation(
            summary = "만족도 리뷰 제출",
            description = "만족도 별점과 선택적 리뷰 내용을 제출한다."
    )
    @PostMapping("/api/info/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<SatisfactionReviewCreateResponse>> createReview(
            @Valid @RequestBody SatisfactionReviewCreateRequest request
    ) {
        SatisfactionReviewCreateResponse response = satisfactionReviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
