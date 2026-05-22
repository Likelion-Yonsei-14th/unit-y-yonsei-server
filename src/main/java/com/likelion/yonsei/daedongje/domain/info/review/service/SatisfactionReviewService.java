package com.likelion.yonsei.daedongje.domain.info.review.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.review.dto.SatisfactionReviewCreateResponse;
import com.likelion.yonsei.daedongje.domain.info.review.entity.SatisfactionReview;
import com.likelion.yonsei.daedongje.domain.info.review.exception.SatisfactionReviewErrorCode;
import com.likelion.yonsei.daedongje.domain.info.review.repository.SatisfactionReviewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SatisfactionReviewService {

    private static final Logger log = LoggerFactory.getLogger(SatisfactionReviewService.class);

    /** 동일 IP 당 1분 동안 허용하는 최대 만족도 리뷰 제출 수. */
    private static final int MAX_SUBMISSIONS_PER_WINDOW = 3;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:satisfaction-review:";

    private final SatisfactionReviewRepository satisfactionReviewRepository;
    /**
     * Redis 자동 설정이 제외된 환경(예: 테스트)에서는 {@link StringRedisTemplate} 빈이 없을 수 있으므로
     * {@link ObjectProvider} 로 선택적으로 주입받는다.
     */
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /** 리뷰 제출 완료 후 프론트에서 안내할 공식 인스타그램 URL. 변경 시 재배포 없이 환경설정으로 교체한다. */
    @Value("${app.social.instagram-url:https://www.instagram.com/likelion_yonsei}")
    private String instagramUrl;

    @Transactional
    public SatisfactionReviewCreateResponse createReview(SatisfactionReviewCreateRequest request, String clientIp) {
        // 로그인 없이 열려 있는 엔드포인트이므로, 무분별한 제출로부터 통계·DB 를 보호하기 위해 먼저 레이트 리밋을 확인한다.
        checkRateLimit(clientIp);

        SatisfactionReview review = SatisfactionReview.create(request.rating(), request.content());
        return SatisfactionReviewCreateResponse.of(satisfactionReviewRepository.save(review), instagramUrl);
    }

    /**
     * IP 기준으로 1분 동안의 만족도 리뷰 제출 수를 제한한다.
     *
     * <p>Redis 의 원자적 {@code INCR} 로 카운트를 올리고, 윈도우의 첫 요청에만 TTL 을 설정한다.
     * Redis 가 구성되지 않았거나({@link StringRedisTemplate} 빈 부재) 장애가 발생한 경우에는
     * 제출 자체를 막지 않도록 fail-open 으로 동작한다.
     */
    private void checkRateLimit(String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            // Redis 자동 설정이 제외된 환경에서는 레이트 리밋을 건너뛴다.
            return;
        }

        String key = RATE_LIMIT_KEY_PREFIX + clientIp;

        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, RATE_LIMIT_WINDOW);
            }
        } catch (RuntimeException e) {
            log.warn("만족도 리뷰 레이트 리밋 확인 실패, 요청을 허용한다. ip={}", clientIp, e);
            return;
        }

        if (count != null && count > MAX_SUBMISSIONS_PER_WINDOW) {
            throw new BusinessException(SatisfactionReviewErrorCode.SATISFACTION_REVIEW_RATE_LIMITED);
        }
    }
}
