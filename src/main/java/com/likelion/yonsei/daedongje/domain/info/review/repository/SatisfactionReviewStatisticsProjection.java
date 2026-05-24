package com.likelion.yonsei.daedongje.domain.info.review.repository;

public interface SatisfactionReviewStatisticsProjection {

    Long getTotalCount();

    Double getAverageRating();

    Long getOneStarCount();

    Long getTwoStarCount();

    Long getThreeStarCount();

    Long getFourStarCount();

    Long getFiveStarCount();
}