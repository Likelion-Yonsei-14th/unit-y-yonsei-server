package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Grafana Cloud 알림 웹훅으로 받은 "현재 발생 중인 알림"을 Redis 해시에 저장·조회한다.
 *
 * <p>키 {@code monitoring:active-alerts}의 field=fingerprint, value=알림 JSON.
 * firing이면 upsert(+키 TTL 갱신), resolved이면 제거. 누락된 resolved에 대비해
 * 키 전체에 TTL을 둔다(매 upsert마다 갱신). Redis 미가용 시 fail-safe로 동작한다.
 */
@Component
public class ActiveAlertStore {

    private static final Logger log = LoggerFactory.getLogger(ActiveAlertStore.class);
    private static final String KEY = "monitoring:active-alerts";
    private static final Duration KEY_TTL = Duration.ofHours(6);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public ActiveAlertStore(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                            ObjectMapper objectMapper) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    public void upsert(ActiveAlertResponse alert) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForHash().put(KEY, alert.fingerprint(), objectMapper.writeValueAsString(alert));
            redis.expire(KEY, KEY_TTL);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("활성 알림 저장 실패. fingerprint={}", alert.fingerprint(), e);
        }
    }

    public void remove(String fingerprint) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForHash().delete(KEY, fingerprint);
        } catch (RuntimeException e) {
            log.warn("활성 알림 삭제 실패. fingerprint={}", fingerprint, e);
        }
    }

    public List<ActiveAlertResponse> findAllActive() {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return List.of();
        }
        try {
            Map<Object, Object> raw = redis.opsForHash().entries(KEY);
            List<ActiveAlertResponse> result = new ArrayList<>();
            for (Object value : raw.values()) {
                result.add(objectMapper.readValue(value.toString(), ActiveAlertResponse.class));
            }
            result.sort(Comparator.comparing(ActiveAlertResponse::startsAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return result;
        } catch (Exception e) {
            log.warn("활성 알림 조회 실패", e);
            return List.of();
        }
    }
}
