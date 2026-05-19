package com.likelion.yonsei.daedongje.domain.info.review.dto;

import com.likelion.yonsei.daedongje.domain.info.review.entity.SatisfactionReview;

import java.time.LocalDateTime;

public record SatisfactionReviewCreateResponse(
        Long id,
        Integer rating,
        String content,
        String instagramUrl,
        LocalDateTime createdAt
) {

    public static SatisfactionReviewCreateResponse of(SatisfactionReview review, String instagramUrl) {
        return new SatisfactionReviewCreateResponse(
                review.getId(),
                review.getRating(),
                review.getContent(),
                instagramUrl,
                review.getCreatedAt()
        );
    }
}
