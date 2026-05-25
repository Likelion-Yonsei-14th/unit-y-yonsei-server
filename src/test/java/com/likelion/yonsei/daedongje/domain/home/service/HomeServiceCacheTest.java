package com.likelion.yonsei.daedongje.domain.home.service;

import com.likelion.yonsei.daedongje.config.CacheConfig;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link HomeService#getPopularBooths()} 의 {@code @Cacheable} 동작 회귀 테스트.
 *
 * <p>단위 테스트(Mockito)는 Spring AOP 프록시가 적용되지 않아 캐시가 검증되지 않으므로,
 * Spring 컨텍스트를 띄워 캐시 설정·캐시명·프록시 적용을 보장한다. 같은 키(파라미터 없음)에 대해
 * 두 번 호출해도 집계 쿼리({@code findPopularBooths})가 한 번만 실행되면 캐시가 적용된 것이다.
 */
@SpringBootTest
@DisplayName("HomeService 인기부스 캐시")
class HomeServiceCacheTest {

    @Autowired
    private HomeService homeService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private BoothClickLogRepository boothClickLogRepository;

    @BeforeEach
    void clearCache() {
        // 캐시는 애플리케이션 스코프 싱글톤이라 테스트 간 상태가 누적되므로 매 테스트 전에 비운다.
        Cache cache = cacheManager.getCache(CacheConfig.POPULAR_BOOTHS);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("두 번 호출해도 클릭로그 집계 쿼리는 한 번만 실행된다(@Cacheable 적용 검증)")
    void getPopularBooths_caches_result() {
        given(boothClickLogRepository.findPopularBooths(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(List.of());

        homeService.getPopularBooths();
        List<?> second = homeService.getPopularBooths();

        assertThat(second).isEmpty();
        // 2회차는 캐시 히트라 집계 쿼리가 재실행되지 않아야 한다.
        verify(boothClickLogRepository, times(1))
                .findPopularBooths(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
    }
}
