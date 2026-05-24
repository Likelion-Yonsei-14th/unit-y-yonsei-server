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
    // NOTE: 키 TTL은 누락된 resolved 웹훅에 대한 안전망이다. 단, 6시간 동안 신규 firing upsert가
    //       없으면 아직 발화 중인 알림도 함께 만료되므로, Grafana 알림 repeat_interval을
    //       6시간 미만으로 두어야 발화 중 알림 표시가 유지된다.
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
            // NOTE: HSET과 EXPIRE는 비원자적이라 둘 사이 프로세스 종료 시 TTL이 갱신 안 될 수 있으나,
            //       모니터링용 best-effort 데이터라 허용한다.
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
        Map<Object, Object> raw;
        try {
            raw = redis.opsForHash().entries(KEY);
        } catch (RuntimeException e) {
            // Redis 연결/조회 장애는 fail-safe로 빈 목록 반환.
            log.warn("활성 알림 조회 실패(Redis)", e);
            return List.of();
        }
        List<ActiveAlertResponse> result = new ArrayList<>();
        for (Object value : raw.values()) {
            try {
                result.add(objectMapper.readValue(value.toString(), ActiveAlertResponse.class));
            } catch (JsonProcessingException e) {
                // 역직렬화 실패는 인프라 장애가 아니라 스키마/프로그래밍 오류 신호 →
                // 해당 항목만 건너뛰고 ERROR로 표면화(전체 목록을 비우지 않음).
                log.error("활성 알림 역직렬화 실패 — 항목 건너뜀. value={}", value, e);
            }
        }
        // NOTE: startsAt 렉시컬 정렬은 Grafana Cloud가 항상 UTC(Z-suffix) ISO-8601로 전송함을
        //       전제한다. +HH:MM 오프셋 형식이 혼입되면 정렬 순서가 깨진다.
        result.sort(Comparator.comparing(ActiveAlertResponse::startsAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }
}
