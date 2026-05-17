// config/SessionConfig.java
package com.likelion.yonsei.daedongje.domain.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Configuration
@EnableRedisIndexedHttpSession( // Redis 세션 설정, 안 하면 HttpSession 조회함
    // maxInactiveIntervalInSeconds = -1,  마지막 요청으로 부터 세션 유지 시간 정하는 코드, -1(영구), 로그인 후 특정 요청 없으면 세션 만료
    redisNamespace = "daedongje:session"
)
public class SessionConfig {
}