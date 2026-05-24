package com.likelion.yonsei.daedongje.domain.info.review.repository;

import com.likelion.yonsei.daedongje.domain.info.review.entity.SatisfactionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SatisfactionReviewRepository extends JpaRepository<SatisfactionReview, Long> {

    @Query("""
        select
            count(review) as totalCount,
            avg(review.rating) as averageRating,
            coalesce(sum(case when review.rating = 1 then 1L else 0L end), 0L) as oneStarCount,
            coalesce(sum(case when review.rating = 2 then 1L else 0L end), 0L) as twoStarCount,
            coalesce(sum(case when review.rating = 3 then 1L else 0L end), 0L) as threeStarCount,
            coalesce(sum(case when review.rating = 4 then 1L else 0L end), 0L) as fourStarCount,
            coalesce(sum(case when review.rating = 5 then 1L else 0L end), 0L) as fiveStarCount
        from SatisfactionReview review
        """)
    SatisfactionReviewStatisticsProjection findStatistics();
}