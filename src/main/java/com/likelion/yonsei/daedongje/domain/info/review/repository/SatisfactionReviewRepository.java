package com.likelion.yonsei.daedongje.domain.info.review.repository;

import com.likelion.yonsei.daedongje.domain.info.review.entity.SatisfactionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SatisfactionReviewRepository extends JpaRepository<SatisfactionReview, Long> {

    long countByRating(Integer rating);

    @Query("select avg(review.rating) from SatisfactionReview review")
    Double findAverageRating();
}