package com.likelion.yonsei.daedongje.domain.info.review.service;

import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateResponse;
import com.likelion.yonsei.daedongje.domain.info.review.entity.SatisfactionReview;
import com.likelion.yonsei.daedongje.domain.info.review.repository.SatisfactionReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SatisfactionReviewService {

    private final SatisfactionReviewRepository satisfactionReviewRepository;

    /** 리뷰 제출 완료 후 프론트에서 안내할 공식 인스타그램 URL. 변경 시 재배포 없이 환경설정으로 교체한다. */
    @Value("${app.social.instagram-url:https://www.instagram.com/likelion_yonsei}")
    private String instagramUrl;

    @Transactional
    public SatisfactionReviewCreateResponse createReview(SatisfactionReviewCreateRequest request) {
        SatisfactionReview review = SatisfactionReview.create(request.rating(), request.content());
        return SatisfactionReviewCreateResponse.of(satisfactionReviewRepository.save(review), instagramUrl);
    }
}
