package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothClickLog;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoothClickLogServiceTest {

    private static final String CLIENT_IP = "127.0.0.1";

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private BoothClickLogRepository boothClickLogRepository;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BoothClickLogService boothClickLogService;

    @Test
    @DisplayName("레이트 리밋 내 요청이면 존재하는 부스의 클릭 로그를 저장한다")
    void createSavesClickLog() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(boothRepository.existsById(1L)).thenReturn(true);

        boothClickLogService.create(1L, CLIENT_IP);

        ArgumentCaptor<BoothClickLog> captor = ArgumentCaptor.forClass(BoothClickLog.class);
        verify(boothClickLogRepository).save(captor.capture());

        BoothClickLog savedLog = captor.getValue();
        assertThat(savedLog.getBoothId()).isEqualTo(1L);
        assertThat(savedLog.getClickedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 부스면 클릭 로그를 저장하지 않고 예외를 던진다")
    void createThrowsWhenBoothNotFound() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(boothRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> boothClickLogService.create(999L, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(BoothErrorCode.BOOTH_NOT_FOUND);

        verify(boothClickLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("레이트 리밋 한도를 초과하면 부스 조회 없이 예외를 던진다")
    void createThrowsWhenRateLimitExceeded() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(11L);

        assertThatThrownBy(() -> boothClickLogService.create(1L, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(BoothErrorCode.BOOTH_CLICK_RATE_LIMITED);

        verify(boothRepository, never()).existsById(org.mockito.ArgumentMatchers.anyLong());
        verify(boothClickLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Redis 장애 시에는 레이트 리밋을 건너뛰고 클릭 로그를 저장한다")
    void createAllowsWhenRedisFails() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        when(boothRepository.existsById(1L)).thenReturn(true);

        boothClickLogService.create(1L, CLIENT_IP);

        verify(boothClickLogRepository).save(org.mockito.ArgumentMatchers.any(BoothClickLog.class));
    }

    @Test
    @DisplayName("Redis 가 구성되지 않은 환경에서는 레이트 리밋을 건너뛰고 클릭 로그를 저장한다")
    void createSkipsRateLimitWhenRedisNotConfigured() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
        when(boothRepository.existsById(1L)).thenReturn(true);

        boothClickLogService.create(1L, CLIENT_IP);

        verify(boothClickLogRepository).save(org.mockito.ArgumentMatchers.any(BoothClickLog.class));
    }
}
