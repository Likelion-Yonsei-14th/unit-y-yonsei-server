package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveAlertStoreTest {

    private static final String KEY = "monitoring:active-alerts";

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ActiveAlertStore store;

    @BeforeEach
    void setUp() {
        store = new ActiveAlertStore(redisTemplateProvider, objectMapper);
    }

    @Test
    @DisplayName("upsert는 fingerprint 필드에 JSON을 저장하고 키 TTL을 갱신한다")
    void upsertWritesJsonAndRefreshesTtl() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        store.upsert(new ActiveAlertResponse("fp1", "HighErrorRate", "high", "5xx>5%", "2026-05-24T10:00:00Z"));

        verify(hashOperations).put(eq(KEY), eq("fp1"), contains("HighErrorRate"));
        verify(redisTemplate).expire(eq(KEY), eq(Duration.ofHours(6)));
    }

    @Test
    @DisplayName("remove는 fingerprint 필드를 삭제한다")
    void removeDeletesField() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        store.remove("fp1");

        verify(hashOperations).delete(KEY, "fp1");
    }

    @Test
    @DisplayName("findAllActive는 JSON을 역직렬화하고 startsAt 내림차순(최신 먼저)으로 정렬한다")
    void findAllDeserializesAndSorts() throws Exception {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        Map<Object, Object> raw = new HashMap<>();
        raw.put("fp1", objectMapper.writeValueAsString(
                new ActiveAlertResponse("fp1", "A", "high", "s1", "2026-05-24T09:00:00Z")));
        raw.put("fp2", objectMapper.writeValueAsString(
                new ActiveAlertResponse("fp2", "B", "medium", "s2", "2026-05-24T10:00:00Z")));
        when(hashOperations.entries(KEY)).thenReturn(raw);

        List<ActiveAlertResponse> result = store.findAllActive();

        assertThat(result).extracting(ActiveAlertResponse::fingerprint).containsExactly("fp2", "fp1");
    }

    @Test
    @DisplayName("Redis 미가용이면 빈 목록을 반환하고 쓰기는 no-op이다")
    void redisUnavailableIsFailSafe() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        assertThat(store.findAllActive()).isEmpty();
        store.upsert(new ActiveAlertResponse("fp", "n", "s", "sum", "t")); // 예외 없이 통과
        store.remove("fp");
    }

    @Test
    @DisplayName("Redis 조회 중 예외가 나면 빈 목록을 반환한다(fail-safe)")
    void findAllActiveSwallowsRedisException() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(KEY)).thenThrow(new RuntimeException("redis down"));

        assertThat(store.findAllActive()).isEmpty();
    }

    @Test
    @DisplayName("역직렬화 불가한 항목은 건너뛰고 정상 항목만 반환한다")
    void findAllActiveSkipsMalformedEntries() throws Exception {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        Map<Object, Object> raw = new HashMap<>();
        raw.put("bad", "{not json");
        raw.put("fp1", objectMapper.writeValueAsString(
                new ActiveAlertResponse("fp1", "A", "high", "s1", "2026-05-24T09:00:00Z")));
        when(hashOperations.entries(KEY)).thenReturn(raw);

        List<ActiveAlertResponse> result = store.findAllActive();

        assertThat(result).extracting(ActiveAlertResponse::fingerprint).containsExactly("fp1");
    }
}
