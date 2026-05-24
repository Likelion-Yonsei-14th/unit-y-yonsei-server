package com.likelion.yonsei.daedongje.domain.info.review.dto;

import com.likelion.yonsei.daedongje.common.response.PageResponse;
import lombok.Getter;

@Getter
public class SatisfactionReviewAdminResponse {

    private final long totalCount;
    private final double averageRating;
    private final RatingDistribution ratingDistribution;
    private final PageResponse<SatisfactionReviewAdminReviewResponse> reviews;

    private SatisfactionReviewAdminResponse(
            long totalCount,
            double averageRating,
            RatingDistribution ratingDistribution,
            PageResponse<SatisfactionReviewAdminReviewResponse> reviews
    ) {
        this.totalCount = totalCount;
        this.averageRating = averageRating;
        this.ratingDistribution = ratingDistribution;
        this.reviews = reviews;
    }

    public static SatisfactionReviewAdminResponse of(
            long totalCount,
            double averageRating,
            RatingDistribution ratingDistribution,
            PageResponse<SatisfactionReviewAdminReviewResponse> reviews
    ) {
        return new SatisfactionReviewAdminResponse(
                totalCount,
                averageRating,
                ratingDistribution,
                reviews
        );
    }

    @Getter
    public static class RatingDistribution {

        private final long oneStarCount;
        private final long twoStarCount;
        private final long threeStarCount;
        private final long fourStarCount;
        private final long fiveStarCount;

        public RatingDistribution(
                long oneStarCount,
                long twoStarCount,
                long threeStarCount,
                long fourStarCount,
                long fiveStarCount
        ) {
            this.oneStarCount = oneStarCount;
            this.twoStarCount = twoStarCount;
            this.threeStarCount = threeStarCount;
            this.fourStarCount = fourStarCount;
            this.fiveStarCount = fiveStarCount;
        }
    }
}