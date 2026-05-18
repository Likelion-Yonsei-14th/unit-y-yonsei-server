  // config/SessionConfig.java
  package com.likelion.yonsei.daedongje.domain.auth.config;

  import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

  @Configuration
  // Redis 세션은 store-type=redis 일 때만 활성화한다.
  // 테스트 프로파일은 store-type=none 이므로 이 설정이 로딩되지 않아
  // Redis 없이 컨텍스트가 뜬다. 운영(redis) / 값 미설정 시에는 활성화.
  @ConditionalOnProperty(name = "spring.session.store-type", havingValue = "redis", matchIfMissing = true)
  @EnableRedisIndexedHttpSession( // Redis 세션 설정, 안 하면 HttpSession 조회함
    // maxInactiveIntervalInSeconds = -1 마지막 요청으로 부터 세션 유지 시간 정하는 코드, -1(영구), 로그인 후 특정 요청 없으면 세션 만료
    redisNamespace = "daedongje:session"
  ) 
  public class SessionConfig {
  }