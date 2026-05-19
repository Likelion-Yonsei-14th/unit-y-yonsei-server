package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothClickLog;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional(readOnly = true)
public class BoothClickLogService {

    private static final Logger log = LoggerFactory.getLogger(BoothClickLogService.class);

    /** 클릭 로그 적재 시각은 인기 부스 집계 윈도우와 동일한 축제 기준 시간대로 기록한다. */
    private static final ZoneId FESTIVAL_ZONE = ZoneId.of("Asia/Seoul");

    /** 동일 IP·부스 조합당 1분 동안 허용하는 최대 클릭 로그 요청 수. */
    private static final int MAX_CLICKS_PER_WINDOW = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:booth-click:";

    private final BoothRepository boothRepository;
    private final BoothClickLogRepository boothClickLogRepository;
    /**
     * Redis 자동 설정이 제외된 환경(예: 테스트)에서는 {@link StringRedisTemplate} 빈이 없을 수 있으므로
     * {@link ObjectProvider} 로 선택적으로 주입받는다.
     */
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public BoothClickLogService(BoothRepository boothRepository,
                                BoothClickLogRepository boothClickLogRepository,
                                ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.boothRepository = boothRepository;
        this.boothClickLogRepository = boothClickLogRepository;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Transactional
    public void create(Long boothId, String clientIp) {
        // 무분별한 클릭 로그 적재로부터 DB 를 보호하기 위해, 존재 여부 조회·저장보다 먼저 레이트 리밋을 확인한다.
        checkRateLimit(boothId, clientIp);

        if (!boothRepository.existsById(boothId)) {
            throw new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND);
        }

        boothClickLogRepository.save(BoothClickLog.create(boothId, LocalDateTime.now(FESTIVAL_ZONE)));
    }

    /**
     * IP·부스 조합 기준으로 1분 동안의 클릭 로그 요청 수를 제한한다.
     *
     * <p>Redis 의 원자적 {@code INCR} 로 카운트를 올리고, 윈도우의 첫 요청에만 TTL 을 설정한다.
     * Redis 가 구성되지 않았거나({@link StringRedisTemplate} 빈 부재) 장애가 발생한 경우에는
     * 클릭 로그 저장 자체를 막지 않도록 fail-open 으로 동작한다.
     */
    private void checkRateLimit(Long boothId, String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            // Redis 자동 설정이 제외된 환경에서는 레이트 리밋을 건너뛴다.
            return;
        }

        String key = RATE_LIMIT_KEY_PREFIX + clientIp + ":" + boothId;

        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, RATE_LIMIT_WINDOW);
            }
        } catch (RuntimeException e) {
            log.warn("부스 클릭 레이트 리밋 확인 실패, 요청을 허용한다. boothId={}, ip={}", boothId, clientIp, e);
            return;
        }

        if (count != null && count > MAX_CLICKS_PER_WINDOW) {
            throw new BusinessException(BoothErrorCode.BOOTH_CLICK_RATE_LIMITED);
        }
    }
}
