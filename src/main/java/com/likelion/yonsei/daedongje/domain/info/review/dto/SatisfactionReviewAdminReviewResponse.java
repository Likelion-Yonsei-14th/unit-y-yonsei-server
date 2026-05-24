package com.likelion.yonsei.daedongje.domain.info.review.dto;

import com.likelion.yonsei.daedongje.domain.info.review.entity.SatisfactionReview;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SatisfactionReviewAdminReviewResponse {

    private final Integer rating;
    private final String content;
    private final LocalDateTime createdAt;

    private SatisfactionReviewAdminReviewResponse(
            Integer rating,
            String content,
            LocalDateTime createdAt
    ) {
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static SatisfactionReviewAdminReviewResponse from(SatisfactionReview review) {
        return new SatisfactionReviewAdminReviewResponse(
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}