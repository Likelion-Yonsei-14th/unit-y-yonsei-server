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

        private final long one;
        private final long two;
        private final long three;
        private final long four;
        private final long five;

        public RatingDistribution(
                long one,
                long two,
                long three,
                long four,
                long five
        ) {
            this.one = one;
            this.two = two;
            this.three = three;
            this.four = four;
            this.five = five;
        }
    }
}