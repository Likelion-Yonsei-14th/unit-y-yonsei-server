package com.likelion.yonsei.daedongje.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 캐시 설정 (Caffeine 인메모리).
 *
 * <p>현재 캐싱 대상은 홈 '오늘의 인기 부스'({@code GET /api/home/popular-booths}) 응답뿐이다.
 * 인기 부스 집계는 {@code booth_click_logs} 를 시간 윈도우로 필터해 booth_id 로 GROUP BY/정렬하는데,
 * 클릭 로그는 부스 조회마다 INSERT 되어 축제 중 무한히 누적된다. 부하 테스트에서 클릭 30만 행 기준
 * 매 호출 ~220ms(풀스캔+temporary+filesort) 가 측정됐다. 랭킹 자체는 초 단위로 바뀌지 않으므로,
 * 짧은 TTL 캐시로 매 호출의 집계 비용을 제거한다(캐시 히트 시 DB 미접근).
 *
 * <p>단일 컨테이너 배포라 인스턴스-로컬 Caffeine 으로 충분하다. 다중 인스턴스로 확장하면
 * Redis 기반 캐시로 교체를 검토한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 홈 인기 부스 응답 캐시 이름. {@code @Cacheable} 에서 동일 상수를 참조한다. */
    public static final String POPULAR_BOOTHS = "popularBooths";

    /** 인기 부스 랭킹 staleness 허용 한도. 이 시간만큼은 집계를 재실행하지 않는다. */
    private static final Duration POPULAR_BOOTHS_TTL = Duration.ofSeconds(60);

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(POPULAR_BOOTHS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(POPULAR_BOOTHS_TTL)
                .maximumSize(16));
        return cacheManager;
    }
}
