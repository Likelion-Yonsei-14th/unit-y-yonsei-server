package com.likelion.yonsei.daedongje.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 시각 의존 로직(라이브 공연 판정 등)이 테스트에서 결정적이도록 Clock 을 빈으로 주입한다.
 * 운영에선 Asia/Seoul 시스템 시계, 테스트에선 고정 Clock 으로 오버라이드한다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
