package com.likelion.yonsei.daedongje.domain.info.review.service;

import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateResponse;
import com.likelion.yonsei.daedongje.domain.info.review.entity.SatisfactionReview;
import com.likelion.yonsei.daedongje.domain.info.review.repository.SatisfactionReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SatisfactionReviewService {

    private static final String INSTAGRAM_URL = "https://www.instagram.com/likelion_yonsei";

    private final SatisfactionReviewRepository satisfactionReviewRepository;

    @Transactional
    public SatisfactionReviewCreateResponse createReview(SatisfactionReviewCreateRequest request) {
        SatisfactionReview review = SatisfactionReview.create(request.rating(), request.content());
        return SatisfactionReviewCreateResponse.of(satisfactionReviewRepository.save(review), INSTAGRAM_URL);
    }
}
