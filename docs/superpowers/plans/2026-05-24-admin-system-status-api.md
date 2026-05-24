# Admin System Status API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자 프론트가 소비할 "시스템 상태" 백엔드 API를 구현한다 — 라이브 헬스 스냅샷, 최근 ERROR 로그, 현재 활성 알림(Grafana 웹훅 수신).

**Architecture:** 새 `domain/monitoring` 모듈. 라이브 지표는 앱이 자기 자신의 Micrometer `MeterRegistry`·Actuator `HealthEndpoint`에서 **인-프로세스로** 읽는다(외부 의존 0). 최근 ERROR 로그는 커스텀 Logback 어펜더가 적재하는 메모리 링버퍼에서 읽는다. 활성 알림은 Grafana Cloud가 웹훅으로 보내주면 Redis 해시에 TTL과 함께 저장하고 관리자 API가 읽는다.

**Tech Stack:** Spring Boot 3.5.13, Java 17, Micrometer, Logback, Spring Data Redis(`StringRedisTemplate`), 기존 관리자 세션 가드(`@RequireAdminRole`), `ApiResponse` 래퍼, `BusinessException`/`GlobalExceptionHandler`. 테스트: JUnit5 + Mockito5 + AssertJ, `@WebMvcTest` 슬라이스.

**상위 스펙:** `docs/superpowers/specs/2026-05-24-server-observability-design.md` (Phase 3에 해당). Phase 1·2(텔레메트리 인프라·Grafana 알림 룰)는 **별도 plan**으로 다룬다. 이 plan은 외부 선행조건이 없다 — Grafana Cloud 없이도 빌드·테스트·배포 가능하며, health·errors는 즉시 동작한다.

---

## File Structure

신규 모듈 — 기존 `domain/booth` 레이아웃(controller/service/dto/exception) 미러.

```
src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/
├── controller/
│   ├── SystemStatusController.java     # 관리자 GET 3종 (@RequireAdminRole)
│   └── AlertWebhookController.java     # 내부 POST 웹훅 (시크릿 검증)
├── service/
│   ├── SystemHealthService.java        # MeterRegistry/HealthEndpoint/BuildProperties 인-프로세스 집계
│   ├── RecentErrorLogService.java      # 링버퍼 읽기 래퍼(컨트롤러 테스트 모킹용 빈)
│   └── ActiveAlertStore.java           # Redis 해시 기반 활성 알림 저장/조회
├── dto/
│   ├── SystemHealthResponse.java       # record (+ 중첩 MemoryInfo/DbPoolInfo)
│   ├── ActiveAlertResponse.java        # record
│   └── GrafanaWebhookRequest.java      # record (+ 중첩 Alert), Grafana payload 매핑
├── log/
│   ├── ErrorLogEntry.java              # record, 링버퍼 항목 = API 응답 형태
│   ├── RecentErrorLogBuffer.java       # 스레드세이프 싱글톤 링버퍼
│   └── RecentErrorLogAppender.java     # Logback AppenderBase → 버퍼 적재
└── exception/
    └── MonitoringErrorCode.java        # MON-001 등

src/main/resources/
├── logback-spring.xml                  # 신규 — 콘솔 기본 + RECENT_ERROR 어펜더 등록
└── application.yaml                     # 수정 — monitoring.webhook.secret 추가

src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/   # 미러 테스트
.env.example                            # 수정 — MONITORING_WEBHOOK_SECRET 추가
```

**책임 경계:** `log/`는 로그 캡처(어펜더+버퍼)만, `service/`는 읽기·집계·Redis만, `controller/`는 HTTP·인증·시크릿만. `RecentErrorLogBuffer`는 순수 싱글톤(어펜더가 write), `RecentErrorLogService`가 그것을 읽는 스프링 빈이라 컨트롤러 슬라이스 테스트에서 모킹된다.

**보안 노트(인프라 plan과 연계):** `/api/admin/system/**`는 기존 인터셉터가 `@RequireAdminRole`로 게이트한다. `/internal/monitoring/alerts`는 Grafana Cloud가 인터넷에서 호출하므로 공개 도달 가능해야 하며(인프라 plan의 Nginx `deny`는 `/actuator`에만 적용, `/internal`은 제외) **공유 시크릿 헤더로 보호**한다. WebConfig 변경 불필요(인터셉터는 `@RequireAdminRole` 없는 경로를 그대로 통과).

---

## Task 1: ErrorLogEntry + RecentErrorLogBuffer (메모리 링버퍼)

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/log/ErrorLogEntry.java`
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogBuffer.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogBufferTest.java`

- [ ] **Step 1: ErrorLogEntry record 작성**

`ErrorLogEntry.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.log;

import java.time.LocalDateTime;

/**
 * 최근 ERROR 로그 1건. 링버퍼 저장 단위이자 관리자 API 응답 형태로 그대로 사용한다.
 * {@code throwable}은 예외가 있으면 "클래스명: 메시지" 형태, 없으면 null.
 */
public record ErrorLogEntry(
        LocalDateTime timestamp,
        String level,
        String logger,
        String message,
        String throwable
) {
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`RecentErrorLogBufferTest.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentErrorLogBufferTest {

    private ErrorLogEntry entry(String message) {
        return new ErrorLogEntry(LocalDateTime.now(), "ERROR", "TestLogger", message, null);
    }

    @Test
    @DisplayName("add한 항목은 최신순(newest first)으로 스냅샷에 담긴다")
    void snapshotIsNewestFirst() {
        RecentErrorLogBuffer buffer = new RecentErrorLogBuffer(3);

        buffer.add(entry("first"));
        buffer.add(entry("second"));

        List<ErrorLogEntry> snapshot = buffer.snapshot();
        assertThat(snapshot).extracting(ErrorLogEntry::message).containsExactly("second", "first");
    }

    @Test
    @DisplayName("용량을 초과하면 가장 오래된 항목을 축출한다")
    void evictsOldestBeyondCapacity() {
        RecentErrorLogBuffer buffer = new RecentErrorLogBuffer(2);

        buffer.add(entry("a"));
        buffer.add(entry("b"));
        buffer.add(entry("c"));

        assertThat(buffer.snapshot()).extracting(ErrorLogEntry::message).containsExactly("c", "b");
    }

    @Test
    @DisplayName("clear는 버퍼를 비운다")
    void clearEmptiesBuffer() {
        RecentErrorLogBuffer buffer = new RecentErrorLogBuffer(5);
        buffer.add(entry("x"));

        buffer.clear();

        assertThat(buffer.snapshot()).isEmpty();
    }
}
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogBufferTest"`
Expected: 컴파일 실패 — `RecentErrorLogBuffer` 클래스 없음.

- [ ] **Step 4: RecentErrorLogBuffer 구현**

`RecentErrorLogBuffer.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 최근 ERROR 로그를 메모리에 보관하는 스레드세이프 싱글톤 링버퍼.
 *
 * <p>{@link RecentErrorLogAppender}(Logback)가 write하고
 * {@code RecentErrorLogService}가 read한다. 용량 초과 시 가장 오래된 항목을 축출한다.
 * 이 인스턴스 한정·재시작 시 휘발(영속 로그는 Loki가 담당).
 */
public final class RecentErrorLogBuffer {

    public static final int DEFAULT_CAPACITY = 100;

    private static final RecentErrorLogBuffer INSTANCE = new RecentErrorLogBuffer(DEFAULT_CAPACITY);

    private final int capacity;
    private final Deque<ErrorLogEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger(0);

    // 테스트에서 용량을 지정하기 위해 package-private.
    RecentErrorLogBuffer(int capacity) {
        this.capacity = capacity;
    }

    public static RecentErrorLogBuffer getInstance() {
        return INSTANCE;
    }

    public void add(ErrorLogEntry entry) {
        entries.addFirst(entry);
        if (size.incrementAndGet() > capacity && entries.pollLast() != null) {
            size.decrementAndGet();
        }
    }

    /** 최신순(newest first) 불변 스냅샷. */
    public List<ErrorLogEntry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public void clear() {
        entries.clear();
        size.set(0);
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogBufferTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/log/ErrorLogEntry.java \
        src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogBuffer.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogBufferTest.java
git commit -m "feat: 최근 ERROR 로그 메모리 링버퍼 추가"
git push
```

---

## Task 2: RecentErrorLogAppender (Logback → 버퍼 적재)

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogAppender.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogAppenderTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`RecentErrorLogAppenderTest.java` (테스트가 어펜더와 같은 패키지라 protected `append`를 직접 호출):
```java
package com.likelion.yonsei.daedongje.domain.monitoring.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecentErrorLogAppenderTest {

    @Test
    @DisplayName("append는 로그 이벤트를 ErrorLogEntry로 변환해 싱글톤 버퍼에 적재한다")
    void appendStoresEntryInSingletonBuffer() {
        RecentErrorLogBuffer.getInstance().clear();

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getLoggerName()).thenReturn("com.example.Foo");
        when(event.getFormattedMessage()).thenReturn("boom");
        when(event.getThrowableProxy()).thenReturn(null);

        RecentErrorLogAppender appender = new RecentErrorLogAppender();
        appender.append(event);

        List<ErrorLogEntry> snapshot = RecentErrorLogBuffer.getInstance().snapshot();
        assertThat(snapshot).hasSize(1);
        assertThat(snapshot.get(0).message()).isEqualTo("boom");
        assertThat(snapshot.get(0).level()).isEqualTo("ERROR");
        assertThat(snapshot.get(0).logger()).isEqualTo("com.example.Foo");
        assertThat(snapshot.get(0).throwable()).isNull();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogAppenderTest"`
Expected: 컴파일 실패 — `RecentErrorLogAppender` 없음.

- [ ] **Step 3: RecentErrorLogAppender 구현**

`RecentErrorLogAppender.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * ERROR 레벨 로그를 {@link RecentErrorLogBuffer} 싱글톤에 적재하는 Logback 어펜더.
 * logback-spring.xml 에서 ThresholdFilter(ERROR)와 함께 등록한다.
 */
public class RecentErrorLogAppender extends AppenderBase<ILoggingEvent> {

    private static final ZoneId FESTIVAL_ZONE = ZoneId.of("Asia/Seoul");

    @Override
    protected void append(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        String throwable = (throwableProxy == null)
                ? null
                : throwableProxy.getClassName() + ": " + throwableProxy.getMessage();

        ErrorLogEntry entry = new ErrorLogEntry(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), FESTIVAL_ZONE),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getFormattedMessage(),
                throwable
        );
        RecentErrorLogBuffer.getInstance().add(entry);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogAppenderTest"`
Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogAppender.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogAppenderTest.java
git commit -m "feat: ERROR 로그 링버퍼 적재 Logback 어펜더 추가"
git push
```

---

## Task 3: logback-spring.xml 등록 + 통합 검증

**Files:**
- Create: `src/main/resources/logback-spring.xml`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogWiringTest.java`

**참고:** `logback-spring.xml`은 Spring Boot 로깅 시스템이 처리하므로, 와이어링 검증은 `@SpringBootTest`(컨텍스트 로딩 시 Boot가 이 파일을 적용)에서 한다. 콘솔 어펜더는 Boot 기본(텍스트)을 유지한다 — **인프라 plan(Phase 1)에서 콘솔 인코더를 구조화 JSON(`logging.structured.format.console=ecs`)으로 교체**할 때 이 파일의 CONSOLE 정의를 함께 조정한다(여기서는 건드리지 않음).

- [ ] **Step 1: logback-spring.xml 작성**

`logback-spring.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Spring Boot 기본 콘솔 로깅 유지 (Phase 1 인프라에서 구조화 JSON으로 교체 예정) -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <!-- 최근 ERROR 로그를 메모리 링버퍼에 적재 (관리자 시스템 상태 API용) -->
    <appender name="RECENT_ERROR"
              class="com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="RECENT_ERROR"/>
    </root>
</configuration>
```

- [ ] **Step 2: 실패하는 통합 테스트 작성**

`RecentErrorLogWiringTest.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * logback-spring.xml 이 RECENT_ERROR 어펜더를 실제로 루트에 연결했는지 검증.
 * Spring Boot 컨텍스트가 로딩되어야 logback-spring.xml 이 적용된다.
 */
@SpringBootTest
class RecentErrorLogWiringTest {

    private static final Logger log = LoggerFactory.getLogger(RecentErrorLogWiringTest.class);

    @Test
    @DisplayName("ERROR 로그를 남기면 링버퍼에 적재된다 (어펜더가 루트에 연결됨)")
    void errorLogIsCapturedByAppender() {
        RecentErrorLogBuffer.getInstance().clear();

        log.error("wiring-test-boom");

        List<ErrorLogEntry> snapshot = RecentErrorLogBuffer.getInstance().snapshot();
        assertThat(snapshot)
                .extracting(ErrorLogEntry::message)
                .contains("wiring-test-boom");
    }

    @Test
    @DisplayName("INFO 로그는 ThresholdFilter에 막혀 링버퍼에 적재되지 않는다")
    void infoLogIsFilteredOut() {
        RecentErrorLogBuffer.getInstance().clear();

        log.info("wiring-test-info");

        assertThat(RecentErrorLogBuffer.getInstance().snapshot())
                .extracting(ErrorLogEntry::message)
                .doesNotContain("wiring-test-info");
    }
}
```

- [ ] **Step 3: 테스트 실패 확인 (xml 미작성 시) / 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogWiringTest"`
Expected: Step 1을 먼저 했으므로 PASS. (만약 `@SpringBootTest` 컨텍스트 로딩이 환경 문제로 실패하면, 기존 `@SpringBootTest` 컨텍스트 로딩 테스트와 동일 환경이므로 동일하게 동작해야 한다 — 실패 시 컨텍스트 로딩 이슈를 먼저 해결.)

- [ ] **Step 4: 커밋**

```bash
git add src/main/resources/logback-spring.xml \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/log/RecentErrorLogWiringTest.java
git commit -m "feat: logback-spring.xml에 ERROR 링버퍼 어펜더 등록"
git push
```

---

## Task 4: RecentErrorLogService (링버퍼 읽기 빈)

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/service/RecentErrorLogService.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/service/RecentErrorLogServiceTest.java`

싱글톤 버퍼를 직접 컨트롤러에 주입하면 슬라이스 테스트에서 모킹이 번거롭다. 읽기 전용 스프링 빈으로 감싸 컨트롤러가 이를 주입·테스트에서 `@MockitoBean`으로 대체한다.

- [ ] **Step 1: 실패하는 테스트 작성**

`RecentErrorLogServiceTest.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentErrorLogServiceTest {

    @Test
    @DisplayName("recent()는 싱글톤 버퍼의 최신순 스냅샷을 반환한다")
    void recentReturnsBufferSnapshot() {
        RecentErrorLogBuffer.getInstance().clear();
        RecentErrorLogBuffer.getInstance()
                .add(new ErrorLogEntry(LocalDateTime.now(), "ERROR", "L", "msg", null));

        RecentErrorLogService service = new RecentErrorLogService();

        List<ErrorLogEntry> recent = service.recent();
        assertThat(recent).extracting(ErrorLogEntry::message).containsExactly("msg");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.service.RecentErrorLogServiceTest"`
Expected: 컴파일 실패 — `RecentErrorLogService` 없음.

- [ ] **Step 3: RecentErrorLogService 구현**

`RecentErrorLogService.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogBuffer;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link RecentErrorLogBuffer} 싱글톤을 읽는 스프링 빈.
 * 어펜더는 싱글톤에 write, 이 빈은 read만 한다.
 */
@Service
public class RecentErrorLogService {

    public List<ErrorLogEntry> recent() {
        return RecentErrorLogBuffer.getInstance().snapshot();
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.service.RecentErrorLogServiceTest"`
Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/service/RecentErrorLogService.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/service/RecentErrorLogServiceTest.java
git commit -m "feat: 최근 ERROR 로그 조회 서비스 추가"
git push
```

---

## Task 5: SystemHealthResponse + SystemHealthService (인-프로세스 헬스 스냅샷)

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/dto/SystemHealthResponse.java`
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/service/SystemHealthService.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/service/SystemHealthServiceTest.java`

- [ ] **Step 1: SystemHealthResponse record 작성**

`SystemHealthResponse.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.dto;

/**
 * 서버 라이브 상태 스냅샷. 모든 수치 필드는 해당 메트릭이 없으면 null(부분 실패 허용).
 */
public record SystemHealthResponse(
        String status,          // UP / DOWN / OUT_OF_SERVICE / UNKNOWN
        String version,         // 빌드 버전, nullable
        Long uptimeSeconds,     // nullable
        MemoryInfo heap,
        DbPoolInfo dbPool,
        Integer liveThreads,    // nullable
        Double cpuUsage         // 0.0~1.0, nullable
) {
    public record MemoryInfo(Long usedBytes, Long maxBytes, Double usedRatio) {
    }

    public record DbPoolInfo(Integer active, Integer idle, Integer pending, Integer max) {
    }
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`SystemHealthServiceTest.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemHealthServiceTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<BuildProperties> buildPropsProvider(String version) {
        Properties p = new Properties();
        if (version != null) {
            p.setProperty("version", version);
        }
        BuildProperties props = version == null ? null : new BuildProperties(p);
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(props);
        return provider;
    }

    @Test
    @DisplayName("등록된 메트릭과 헬스/빌드 정보를 스냅샷으로 매핑한다")
    void mapsRegisteredMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Gauge.builder("jvm.memory.used", () -> 500_000_000.0).tag("area", "heap").register(registry);
        Gauge.builder("jvm.memory.max", () -> 1_000_000_000.0).tag("area", "heap").register(registry);
        Gauge.builder("hikaricp.connections.active", () -> 3.0).register(registry);
        Gauge.builder("hikaricp.connections.idle", () -> 5.0).register(registry);
        Gauge.builder("hikaricp.connections.pending", () -> 0.0).register(registry);
        Gauge.builder("hikaricp.connections.max", () -> 10.0).register(registry);
        Gauge.builder("jvm.threads.live", () -> 42.0).register(registry);
        Gauge.builder("system.cpu.usage", () -> 0.3).register(registry);
        Gauge.builder("process.uptime", () -> 3600.0).register(registry);

        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        SystemHealthService service =
                new SystemHealthService(registry, healthEndpoint, buildPropsProvider("1.2.3"));

        SystemHealthResponse r = service.snapshot();

        assertThat(r.status()).isEqualTo("UP");
        assertThat(r.version()).isEqualTo("1.2.3");
        assertThat(r.uptimeSeconds()).isEqualTo(3600L);
        assertThat(r.heap().usedBytes()).isEqualTo(500_000_000L);
        assertThat(r.heap().maxBytes()).isEqualTo(1_000_000_000L);
        assertThat(r.heap().usedRatio()).isEqualTo(0.5);
        assertThat(r.dbPool().active()).isEqualTo(3);
        assertThat(r.dbPool().max()).isEqualTo(10);
        assertThat(r.liveThreads()).isEqualTo(42);
        assertThat(r.cpuUsage()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("메트릭/빌드정보가 없으면 해당 필드는 null이고 상태는 그대로 반영된다")
    void missingMetricsBecomeNull() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.down().build());

        SystemHealthService service =
                new SystemHealthService(registry, healthEndpoint, buildPropsProvider(null));

        SystemHealthResponse r = service.snapshot();

        assertThat(r.status()).isEqualTo("DOWN");
        assertThat(r.version()).isNull();
        assertThat(r.heap().usedBytes()).isNull();
        assertThat(r.heap().usedRatio()).isNull();
        assertThat(r.dbPool().active()).isNull();
        assertThat(r.liveThreads()).isNull();
        assertThat(r.cpuUsage()).isNull();
    }
}
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.service.SystemHealthServiceTest"`
Expected: 컴파일 실패 — `SystemHealthService` 없음.

- [ ] **Step 4: SystemHealthService 구현**

`SystemHealthService.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/**
 * 앱 자신의 Micrometer 메트릭·Actuator 헬스·빌드 정보를 인-프로세스로 읽어
 * 관리자 시스템 상태 스냅샷을 만든다. 외부(Grafana Cloud) 의존 없음.
 */
@Service
public class SystemHealthService {

    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public SystemHealthService(MeterRegistry meterRegistry,
                               HealthEndpoint healthEndpoint,
                               ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.meterRegistry = meterRegistry;
        this.healthEndpoint = healthEndpoint;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    public SystemHealthResponse snapshot() {
        Long heapUsed = gaugeLong("jvm.memory.used", "area", "heap");
        Long heapMax = gaugeLong("jvm.memory.max", "area", "heap");
        Double heapRatio = (heapUsed != null && heapMax != null && heapMax > 0)
                ? (double) heapUsed / heapMax
                : null;

        SystemHealthResponse.MemoryInfo heap =
                new SystemHealthResponse.MemoryInfo(heapUsed, heapMax, heapRatio);

        SystemHealthResponse.DbPoolInfo dbPool = new SystemHealthResponse.DbPoolInfo(
                gaugeInt("hikaricp.connections.active"),
                gaugeInt("hikaricp.connections.idle"),
                gaugeInt("hikaricp.connections.pending"),
                gaugeInt("hikaricp.connections.max")
        );

        return new SystemHealthResponse(
                healthStatus(),
                version(),
                gaugeLong("process.uptime"),
                heap,
                dbPool,
                gaugeInt("jvm.threads.live"),
                gaugeDouble("system.cpu.usage")
        );
    }

    private String healthStatus() {
        try {
            HealthComponent health = healthEndpoint.health();
            Status status = (health == null) ? null : health.getStatus();
            return (status == null) ? null : status.getCode();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String version() {
        BuildProperties props = buildPropertiesProvider.getIfAvailable();
        return (props == null) ? null : props.getVersion();
    }

    /** 이름(+선택 태그 1쌍)에 매칭되는 게이지 값들의 합. 없으면 null. */
    private Double gaugeDouble(String name, String... tags) {
        Search search = meterRegistry.find(name);
        if (tags.length >= 2) {
            search = search.tag(tags[0], tags[1]);
        }
        double sum = 0;
        boolean found = false;
        for (Gauge g : search.gauges()) {
            double v = g.value();
            if (!Double.isNaN(v)) {
                sum += v;
                found = true;
            }
        }
        return found ? sum : null;
    }

    private Long gaugeLong(String name, String... tags) {
        Double v = gaugeDouble(name, tags);
        return (v == null) ? null : (long) (double) v;
    }

    private Integer gaugeInt(String name, String... tags) {
        Double v = gaugeDouble(name, tags);
        return (v == null) ? null : (int) (double) v;
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.service.SystemHealthServiceTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/dto/SystemHealthResponse.java \
        src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/service/SystemHealthService.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/service/SystemHealthServiceTest.java
git commit -m "feat: 인-프로세스 시스템 헬스 스냅샷 서비스 추가"
git push
```

---

## Task 6: MonitoringErrorCode

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/exception/MonitoringErrorCode.java`

`ErrorCode` 인터페이스 구현 enum (순수 데이터, `BoothErrorCode`와 동일 패턴). 전용 테스트 불필요 — Task 7의 웹훅 컨트롤러 테스트가 401 매핑으로 사용 검증.

- [ ] **Step 1: MonitoringErrorCode 작성**

`MonitoringErrorCode.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MonitoringErrorCode implements ErrorCode {

    INVALID_WEBHOOK_SECRET(HttpStatus.UNAUTHORIZED, "MON-001", "유효하지 않은 웹훅 시크릿입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MonitoringErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/exception/MonitoringErrorCode.java
git commit -m "feat: 모니터링 에러코드(MON-001) 추가"
git push
```

---

## Task 7: ActiveAlertResponse + ActiveAlertStore (Redis 활성 알림)

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/dto/ActiveAlertResponse.java`
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/service/ActiveAlertStore.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/service/ActiveAlertStoreTest.java`

Redis 해시 `monitoring:active-alerts` (field=fingerprint, value=JSON). firing→HSET+TTL갱신, resolved→HDEL, 조회→HGETALL. Redis 미가용 시 fail-safe(빈 목록·no-op) — `BoothClickLogService`의 `ObjectProvider<StringRedisTemplate>` 패턴 동일.

- [ ] **Step 1: ActiveAlertResponse record 작성**

`ActiveAlertResponse.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.dto;

/**
 * 현재 발생 중인 알림 1건(관리자 표시용). Redis 해시에 JSON으로 저장된다.
 */
public record ActiveAlertResponse(
        String fingerprint,
        String name,        // Grafana label: alertname
        String severity,    // Grafana label: severity
        String summary,     // Grafana annotation: summary
        String startsAt     // ISO-8601 문자열
) {
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`ActiveAlertStoreTest.java`:
```java
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
import static org.mockito.ArgumentMatchers.any;
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
        verify(redisTemplate).expire(eq(KEY), any(Duration.class));
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
}
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStoreTest"`
Expected: 컴파일 실패 — `ActiveAlertStore` 없음.

- [ ] **Step 4: ActiveAlertStore 구현**

`ActiveAlertStore.java`:
```java
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
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStoreTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/dto/ActiveAlertResponse.java \
        src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/service/ActiveAlertStore.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/service/ActiveAlertStoreTest.java
git commit -m "feat: 활성 알림 Redis 저장소 추가"
git push
```

---

## Task 8: GrafanaWebhookRequest + AlertWebhookController (웹훅 수신 + 시크릿)

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/dto/GrafanaWebhookRequest.java`
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/AlertWebhookController.java`
- Modify: `src/main/resources/application.yaml` (monitoring.webhook.secret 추가)
- Modify: `.env.example` (MONITORING_WEBHOOK_SECRET 추가)
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/AlertWebhookControllerTest.java`

- [ ] **Step 1: GrafanaWebhookRequest record 작성**

`GrafanaWebhookRequest.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Grafana Cloud 알림 웹훅 payload(필요 필드만). 미지의 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrafanaWebhookRequest(
        String status,
        List<Alert> alerts
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(
            String status,                 // firing / resolved
            Map<String, String> labels,    // alertname, severity, ...
            Map<String, String> annotations, // summary, description, ...
            String startsAt,
            String endsAt,
            String fingerprint
    ) {
    }
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`AlertWebhookControllerTest.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AlertWebhookController.class,
        properties = "monitoring.webhook.secret=test-secret")
class AlertWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActiveAlertStore activeAlertStore;

    // 인터셉터 빈 구성을 위해 필요 (BoothControllerTest와 동일 패턴)
    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("올바른 시크릿 + firing 알림이면 활성 알림으로 저장한다")
    void firingStoresAlert() throws Exception {
        String body = """
                {"status":"firing","alerts":[
                  {"status":"firing","labels":{"alertname":"HighErrorRate","severity":"high"},
                   "annotations":{"summary":"5xx>5%"},"startsAt":"2026-05-24T10:00:00Z",
                   "fingerprint":"fp1"}]}""";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<ActiveAlertResponse> captor = ArgumentCaptor.forClass(ActiveAlertResponse.class);
        verify(activeAlertStore).upsert(captor.capture());
        assertThat(captor.getValue().fingerprint()).isEqualTo("fp1");
        assertThat(captor.getValue().name()).isEqualTo("HighErrorRate");
        assertThat(captor.getValue().severity()).isEqualTo("high");
        assertThat(captor.getValue().summary()).isEqualTo("5xx>5%");
    }

    @Test
    @DisplayName("resolved 알림이면 활성 알림에서 제거한다")
    void resolvedRemovesAlert() throws Exception {
        String body = """
                {"status":"resolved","alerts":[
                  {"status":"resolved","fingerprint":"fp1"}]}""";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(activeAlertStore).remove("fp1");
    }

    @Test
    @DisplayName("시크릿이 틀리면 401(MON-001)을 반환하고 저장소를 건드리지 않는다")
    void wrongSecretReturns401() throws Exception {
        String body = "{\"status\":\"firing\",\"alerts\":[]}";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer WRONG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MON-001"));

        verifyNoInteractions(activeAlertStore);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
    void missingHeaderReturns401() throws Exception {
        String body = "{\"status\":\"firing\",\"alerts\":[]}";

        mockMvc.perform(post("/internal/monitoring/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MON-001"));

        verifyNoInteractions(activeAlertStore);
    }
}
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.controller.AlertWebhookControllerTest"`
Expected: 컴파일 실패 — `AlertWebhookController` 없음.

- [ ] **Step 4: AlertWebhookController 구현**

`AlertWebhookController.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.GrafanaWebhookRequest;
import com.likelion.yonsei.daedongje.domain.monitoring.exception.MonitoringErrorCode;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Grafana Cloud 알림 웹훅 수신(내부용). 관리자 세션이 아니라 공유 시크릿으로 보호한다.
 * firing→활성 알림 저장, resolved→제거.
 */
@Tag(name = "모니터링 웹훅", description = "Grafana Cloud 알림 웹훅 수신 (내부용)")
@RestController
@RequestMapping("/internal/monitoring")
public class AlertWebhookController {

    private static final String FIRING = "firing";
    private static final String RESOLVED = "resolved";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ActiveAlertStore activeAlertStore;
    private final String webhookSecret;

    public AlertWebhookController(ActiveAlertStore activeAlertStore,
                                  @Value("${monitoring.webhook.secret:}") String webhookSecret) {
        this.activeAlertStore = activeAlertStore;
        this.webhookSecret = webhookSecret;
    }

    @Operation(summary = "Grafana 알림 웹훅 수신")
    @PostMapping("/alerts")
    public ApiResponse<Void> receive(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody GrafanaWebhookRequest request
    ) {
        verifySecret(authorization);

        if (request.alerts() != null) {
            for (GrafanaWebhookRequest.Alert alert : request.alerts()) {
                applyAlert(alert);
            }
        }
        return ApiResponse.successEmpty();
    }

    private void applyAlert(GrafanaWebhookRequest.Alert alert) {
        if (RESOLVED.equalsIgnoreCase(alert.status())) {
            activeAlertStore.remove(alert.fingerprint());
            return;
        }
        if (FIRING.equalsIgnoreCase(alert.status())) {
            activeAlertStore.upsert(toResponse(alert));
        }
    }

    private ActiveAlertResponse toResponse(GrafanaWebhookRequest.Alert alert) {
        return new ActiveAlertResponse(
                alert.fingerprint(),
                label(alert, "alertname"),
                label(alert, "severity"),
                annotation(alert, "summary"),
                alert.startsAt()
        );
    }

    private String label(GrafanaWebhookRequest.Alert alert, String key) {
        return (alert.labels() == null) ? null : alert.labels().get(key);
    }

    private String annotation(GrafanaWebhookRequest.Alert alert, String key) {
        return (alert.annotations() == null) ? null : alert.annotations().get(key);
    }

    private void verifySecret(String authorization) {
        if (webhookSecret == null || webhookSecret.isBlank()
                || authorization == null || !authorization.equals(BEARER_PREFIX + webhookSecret)) {
            throw new BusinessException(MonitoringErrorCode.INVALID_WEBHOOK_SECRET);
        }
    }
}
```

- [ ] **Step 5: application.yaml에 시크릿 설정 추가**

`src/main/resources/application.yaml`의 최상위 `app:` 블록 바로 아래(같은 들여쓰기 레벨)에 추가:
```yaml
monitoring:
  webhook:
    # Grafana Cloud 웹훅 contact point가 Authorization: Bearer <secret> 로 보내는 값.
    # 미설정 시 모든 웹훅 요청을 401로 거부(안전 기본값).
    secret: ${MONITORING_WEBHOOK_SECRET:}
```

- [ ] **Step 6: .env.example에 환경변수 추가**

`.env.example`의 끝에 추가:
```
MONITORING_WEBHOOK_SECRET=change-me-to-a-strong-random-secret
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.controller.AlertWebhookControllerTest"`
Expected: PASS (4 tests).

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/dto/GrafanaWebhookRequest.java \
        src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/AlertWebhookController.java \
        src/main/resources/application.yaml \
        .env.example \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/AlertWebhookControllerTest.java
git commit -m "feat: Grafana 알림 웹훅 수신 컨트롤러 추가 (시크릿 검증)"
git push
```

---

## Task 9: SystemStatusController (관리자 GET 3종)

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/SystemStatusController.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/SystemStatusControllerTest.java`

클래스 레벨 `@RequireAdminRole({SUPER, MASTER})`로 게이트. 세 GET이 각각 서비스에 위임. 슬라이스 테스트는 `BoothControllerTest`와 동일 패턴(`AdminAuthContextService`·`JpaMetamodelMappingContext` 모킹)으로 구성하고, 인터셉터 통과를 위해 `getCurrentAdmin`이 SUPER 관리자를 반환하도록 스텁한다.

- [ ] **Step 1: 실패하는 테스트 작성**

`SystemStatusControllerTest.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import com.likelion.yonsei.daedongje.domain.monitoring.service.RecentErrorLogService;
import com.likelion.yonsei.daedongje.domain.monitoring.service.SystemHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemStatusController.class)
class SystemStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemHealthService systemHealthService;

    @MockitoBean
    private RecentErrorLogService recentErrorLogService;

    @MockitoBean
    private ActiveAlertStore activeAlertStore;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void authenticateAsSuper() {
        when(adminAuthContextService.getCurrentAdmin(any()))
                .thenReturn(new AdminSessionUser(1L, AdminRole.SUPER, "admin"));
    }

    @Test
    @DisplayName("GET /health 는 시스템 스냅샷을 반환한다")
    void healthReturnsSnapshot() throws Exception {
        SystemHealthResponse.MemoryInfo heap = new SystemHealthResponse.MemoryInfo(500L, 1000L, 0.5);
        SystemHealthResponse.DbPoolInfo pool = new SystemHealthResponse.DbPoolInfo(3, 5, 0, 10);
        when(systemHealthService.snapshot())
                .thenReturn(new SystemHealthResponse("UP", "1.2.3", 3600L, heap, pool, 42, 0.3));

        mockMvc.perform(get("/api/admin/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.heap.usedRatio").value(0.5))
                .andExpect(jsonPath("$.data.dbPool.active").value(3));
    }

    @Test
    @DisplayName("GET /errors 는 최근 ERROR 로그 목록을 반환한다")
    void errorsReturnsRecent() throws Exception {
        when(recentErrorLogService.recent()).thenReturn(List.of(
                new ErrorLogEntry(LocalDateTime.of(2026, 5, 24, 10, 0), "ERROR", "Foo", "boom", null)));

        mockMvc.perform(get("/api/admin/system/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].message").value("boom"))
                .andExpect(jsonPath("$.data[0].level").value("ERROR"));
    }

    @Test
    @DisplayName("GET /alerts 는 현재 활성 알림 목록을 반환한다")
    void alertsReturnsActive() throws Exception {
        when(activeAlertStore.findAllActive()).thenReturn(List.of(
                new ActiveAlertResponse("fp1", "HighErrorRate", "high", "5xx>5%", "2026-05-24T10:00:00Z")));

        mockMvc.perform(get("/api/admin/system/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("HighErrorRate"))
                .andExpect(jsonPath("$.data[0].severity").value("high"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.controller.SystemStatusControllerTest"`
Expected: 컴파일 실패 — `SystemStatusController` 없음.

- [ ] **Step 3: SystemStatusController 구현**

`SystemStatusController.java`:
```java
package com.likelion.yonsei.daedongje.domain.monitoring.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.ActiveAlertResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.service.ActiveAlertStore;
import com.likelion.yonsei.daedongje.domain.monitoring.service.RecentErrorLogService;
import com.likelion.yonsei.daedongje.domain.monitoring.service.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 "시스템 상태" 페이지 백엔드 API. SUPER/MASTER만 접근.
 * 라이브 헬스·최근 ERROR 로그·현재 활성 알림을 인-프로세스/Redis에서 읽어 내려준다.
 */
@Tag(name = "시스템 상태 어드민", description = "서버 라이브 상태·최근 에러·활성 알림 조회 (관리자)")
@RestController
@RequestMapping("/api/admin/system")
@RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER})
public class SystemStatusController {

    private final SystemHealthService systemHealthService;
    private final RecentErrorLogService recentErrorLogService;
    private final ActiveAlertStore activeAlertStore;

    public SystemStatusController(SystemHealthService systemHealthService,
                                  RecentErrorLogService recentErrorLogService,
                                  ActiveAlertStore activeAlertStore) {
        this.systemHealthService = systemHealthService;
        this.recentErrorLogService = recentErrorLogService;
        this.activeAlertStore = activeAlertStore;
    }

    @Operation(summary = "서버 라이브 상태 스냅샷")
    @GetMapping("/health")
    public ApiResponse<SystemHealthResponse> health() {
        return ApiResponse.success(systemHealthService.snapshot());
    }

    @Operation(summary = "최근 ERROR 로그")
    @GetMapping("/errors")
    public ApiResponse<List<ErrorLogEntry>> errors() {
        return ApiResponse.success(recentErrorLogService.recent());
    }

    @Operation(summary = "현재 발생 중인 알림")
    @GetMapping("/alerts")
    public ApiResponse<List<ActiveAlertResponse>> alerts() {
        return ApiResponse.success(activeAlertStore.findAllActive());
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.monitoring.controller.SystemStatusControllerTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/SystemStatusController.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/controller/SystemStatusControllerTest.java
git commit -m "feat: 관리자 시스템 상태 조회 API 추가"
git push
```

---

## Task 10: 전체 검증 + 빌드

**Files:** (없음 — 검증만)

- [ ] **Step 1: 전체 테스트 실행**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — 신규 monitoring 테스트 전부 포함, 기존 테스트 회귀 없음.

- [ ] **Step 2: 전체 빌드(테스트 포함) 실행**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 변경 요약 확인**

Run: `git status` 와 `git log --oneline origin/dev..HEAD`
Expected: monitoring 모듈 신규 파일 + application.yaml/.env.example/logback-spring.xml 수정, 각 Task별 커밋이 push된 상태. 의도 외 파일(`.gstack/` 등)이 staged되지 않았는지 확인.

- [ ] **Step 4: (선택) 로컬 수동 스모크**

로컬 실행 후 확인(선택):
```bash
# 앱 기동(로컬 docker-compose의 MySQL/Redis 필요): ./gradlew bootRun
# 관리자 세션 로그인 쿠키로:
#   GET  http://localhost:8080/api/admin/system/health   → 200 + status/heap/dbPool
#   GET  http://localhost:8080/api/admin/system/errors    → 200 + (에러 발생 시 목록)
#   GET  http://localhost:8080/api/admin/system/alerts     → 200 + []
# 웹훅(시크릿 설정 후):
#   POST http://localhost:8080/internal/monitoring/alerts
#        -H "Authorization: Bearer <MONITORING_WEBHOOK_SECRET>" -d '{"status":"firing","alerts":[...]}'
#        → 200, 이후 /alerts에 반영
```

---

## 후속 (이 plan 범위 밖)

- **인프라 plan (Phase 1·2)** — `micrometer-registry-prometheus` 추가 + `/actuator/prometheus` 노출 + Nginx `/actuator` deny, 구조화 콘솔 로깅(logback-spring.xml CONSOLE 인코더 교체), `docker-compose.prod.yml`에 Alloy 추가, Grafana Cloud 계정·알림 룰·Discord/웹훅 contact point. 별도 plan 문서로 작성하며 **Grafana Cloud 계정**이 선행조건.
- **FE (Phase 4, 타 레포)** — 본 plan이 제공한 `/api/admin/system/*` 계약을 소비하는 관리자 "시스템 상태" 페이지.
- **연동 시 주의** — 인프라 plan에서 Grafana 웹훅 contact point의 Authorization 헤더를 `Bearer <MONITORING_WEBHOOK_SECRET>`로 설정해야 활성 알림이 채워진다.
