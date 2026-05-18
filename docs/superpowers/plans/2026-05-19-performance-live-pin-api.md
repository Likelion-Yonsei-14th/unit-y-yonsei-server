# 공연 라이브 수동 지정 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 운영진(SUPER)이 현재 라이브 공연 1건을 수동 지정·교체·해제하고, 공개 API로 그 값을 조회할 수 있는 엔드포인트를 추가한다.

**Architecture:** 전역 단일 행 포인터 테이블 `live_performance`(행 1개, `performance_id` nullable FK)를 둔다. `LivePerformanceService`가 조회·갱신을 모두 담당하며, 갱신은 싱글톤 행을 upsert 한다(시드 행이 없어도 동작 — 테스트 환경 호환). 공개 `GET`은 기존 `PerformanceReadController`에, SUPER 전용 `PUT`은 신규 `LivePerformanceAdminController`에 둔다. 라이브 포인터는 `performanceStatus`와 직교한다(상태를 바꾸지 않음).

**Tech Stack:** Spring Boot 3, Spring Data JPA, Flyway(MySQL) / H2(test), JUnit 5, Mockito, MockMvc, AssertJ, Gradle.

---

## 배경 참고

설계 문서: `docs/superpowers/specs/2026-05-19-performance-live-pin-api-design.md`

- Linear BAC-89 / GitHub #173 / Function ID P-01
- 결정 사항: ① 라이브 지정은 `performanceStatus`를 바꾸지 않음 ② 전용 단일 행 테이블 ③ `updated_by` 생략 ④ 핀된 공연이 `HIDDEN`이어도 `GET /live`는 그대로 반환

## 주요 환경 제약

- **운영(MySQL)**: Flyway가 `live_performance` 테이블 + 시드 행 1개를 생성한다.
- **테스트(H2)**: `src/test/resources/application.yaml`에서 Flyway가 **비활성**이고 `ddl-auto: create-drop`이라 시드 행이 없다. 따라서 서비스의 갱신 로직은 **싱글톤 행이 없으면 새로 만드는 upsert**여야 테스트와 운영 모두에서 동작한다.

## File Structure

| 파일 | 책임 | 신규/수정 |
|---|---|---|
| `src/main/resources/db/migration/V25__create_live_performance_table.sql` | 테이블 + 시드 행 1개 | 신규 |
| `domain/performance/entity/LivePerformance.java` | 단일 행 포인터 엔티티 | 신규 |
| `domain/performance/repository/LivePerformanceRepository.java` | 라이브 포인터 영속성 | 신규 |
| `domain/performance/dto/LivePerformanceUpdateRequest.java` | `PUT` 요청 바디 | 신규 |
| `domain/performance/service/LivePerformanceService.java` | 라이브 조회·갱신 로직 | 신규 |
| `domain/performance/controller/PerformanceReadController.java` | `GET /api/performances/live` 추가 | 수정 |
| `domain/performance/controller/LivePerformanceAdminController.java` | `PUT /api/admin/performances/live` | 신규 |

패키지 베이스: `com.likelion.yonsei.daedongje`

---

## Task 1: Flyway 마이그레이션 — `live_performance` 테이블

**Files:**
- Create: `src/main/resources/db/migration/V25__create_live_performance_table.sql`

- [ ] **Step 1: 마이그레이션 파일 작성**

기존 마이그레이션(`V9__create_menu_table.sql` 등)의 포맷을 따른다. `id`는 AUTO_INCREMENT가 아니라 고정값 1이며, 시드 행을 함께 INSERT 한다.

```sql
-- 운영진이 수동 지정하는 '현재 라이브 공연' 단일 포인터.
-- 전역 단일 값이므로 행 하나만 두고, performance_id 가 NULL 이면 미지정 상태.
CREATE TABLE live_performance (
    id             BIGINT PRIMARY KEY,
    performance_id BIGINT NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_live_performance_performance
        FOREIGN KEY (performance_id)
        REFERENCES performances(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 서비스가 항상 의존할 수 있도록 싱글톤 행(id = 1)을 미리 시드한다.
INSERT INTO live_performance (id, performance_id, created_at, updated_at)
VALUES (1, NULL, NOW(6), NOW(6));
```

- [ ] **Step 2: 컴파일/마이그레이션 정합성 확인**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL` (SQL 파일은 컴파일 대상이 아니므로 영향 없음 — 파일 경로/이름이 `V25__`로 시작하는지 육안 확인)

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/db/migration/V25__create_live_performance_table.sql
git commit -m "feat: 라이브 공연 포인터 테이블 마이그레이션 추가 (BAC-89)"
git push
```

---

## Task 2: `LivePerformance` 엔티티

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/entity/LivePerformance.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/performance/entity/LivePerformanceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`PerformanceTest`와 같은 위치의 순수 단위 테스트. `singleton()` 팩토리가 id=1·performance=null 인 행을 만들고, `updatePerformance()`가 핀/해제를 모두 처리하는지 검증한다.

```java
package com.likelion.yonsei.daedongje.domain.performance.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LivePerformanceTest {

    @Test
    void singleton_creates_row_with_fixed_id_and_no_performance() {
        LivePerformance livePerformance = LivePerformance.singleton();

        assertThat(livePerformance.getId()).isEqualTo(LivePerformance.SINGLETON_ID);
        assertThat(livePerformance.getPerformance()).isNull();
    }

    @Test
    void updatePerformance_pins_and_clears_performance() {
        LivePerformance livePerformance = LivePerformance.singleton();
        Performance performance = Performance.create(null, "Main Stage");
        ReflectionTestUtils.setField(performance, "id", 12L);

        livePerformance.updatePerformance(performance);
        assertThat(livePerformance.getPerformance()).isEqualTo(performance);

        livePerformance.updatePerformance(null);
        assertThat(livePerformance.getPerformance()).isNull();
    }
}
```

> 참고: `Performance.create(null, ...)`는 `adminUser`가 null이면 예외를 던진다. 위 테스트는 그 경로를 쓰지 않도록 `Performance` 인스턴스가 필요할 뿐이다 — Step 1 실행 시 컴파일 에러(아래 Step 2)가 먼저 나므로, 엔티티 구현 후 테스트가 통과하는지 Step 4에서 확인한다. `Performance.create`가 null adminUser로 실패하면 대신 `ReflectionTestUtils`로 빈 인스턴스를 만든다 — 그러나 `Performance`에는 `@NoArgsConstructor(PROTECTED)`가 있으므로 다음으로 대체한다:
>
> ```java
> Performance performance = (Performance) ReflectionTestUtils
>         .invokeConstructor(Performance.class);
> ReflectionTestUtils.setField(performance, "id", 12L);
> ```
>
> Step 1 작성 시 위 대체 버전(`invokeConstructor`)을 사용하라. `Performance.create(null, ...)`은 쓰지 않는다.

최종 Step 1 테스트의 `updatePerformance_pins_and_clears_performance`는 다음으로 작성한다:

```java
    @Test
    void updatePerformance_pins_and_clears_performance() {
        LivePerformance livePerformance = LivePerformance.singleton();
        Performance performance = (Performance) ReflectionTestUtils.invokeConstructor(Performance.class);
        ReflectionTestUtils.setField(performance, "id", 12L);

        livePerformance.updatePerformance(performance);
        assertThat(livePerformance.getPerformance()).isEqualTo(performance);

        livePerformance.updatePerformance(null);
        assertThat(livePerformance.getPerformance()).isNull();
    }
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformanceTest"`
Expected: 컴파일 실패 — `LivePerformance` 심볼을 찾을 수 없음

- [ ] **Step 3: 엔티티 구현**

`PerformanceImage` 엔티티의 스타일(@Getter, @Entity, @Table, @NoArgsConstructor(PROTECTED), private 생성자 + static 팩토리)을 따른다. `@Id`에 `@GeneratedValue`를 **붙이지 않는다**(고정값 1).

```java
package com.likelion.yonsei.daedongje.domain.performance.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 운영진이 수동 지정하는 '현재 라이브 공연' 단일 포인터.
 *
 * <p>전역 단일 값이므로 항상 {@link #SINGLETON_ID}(= 1) 한 행만 존재한다.
 * {@code performance} 가 null 이면 라이브 미지정 상태다.
 * 라이브 포인터는 {@code Performance.performanceStatus} 와 직교하며, 상태를 바꾸지 않는다.
 */
@Getter
@Entity
@Table(name = "live_performance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LivePerformance extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    private LivePerformance(Long id) {
        this.id = id;
    }

    /** 시드 행이 없을 때(예: 테스트 환경) 서비스가 새로 만들 수 있는 싱글톤 행. */
    public static LivePerformance singleton() {
        return new LivePerformance(SINGLETON_ID);
    }

    /** 라이브 공연을 지정/교체한다. {@code null} 을 넘기면 해제한다. */
    public void updatePerformance(Performance performance) {
        this.performance = performance;
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformanceTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/entity/LivePerformance.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/performance/entity/LivePerformanceTest.java
git commit -m "feat: 라이브 공연 단일 행 포인터 엔티티 추가 (BAC-89)"
git push
```

---

## Task 3: `LivePerformanceRepository`

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/repository/LivePerformanceRepository.java`

리포지토리 인터페이스는 별도 단위 테스트 없이 Task 5(서비스)·Task 6(컨트롤러) 테스트에서 검증된다.

- [ ] **Step 1: 리포지토리 작성**

`PerformanceRepository`의 fetch join 패턴(`findAllWithLocationByPerformanceStatusNot`)을 따른다. 공개 `GET`에서 `performance.location`까지 한 번에 로딩해 N+1을 피한다.

```java
package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LivePerformanceRepository extends JpaRepository<LivePerformance, Long> {

    // 라이브 공연 조회 시 performance 와 location 까지 함께 로딩해 N+1 을 피한다.
    @Query("SELECT lp FROM LivePerformance lp "
            + "LEFT JOIN FETCH lp.performance p "
            + "LEFT JOIN FETCH p.location "
            + "WHERE lp.id = :id")
    Optional<LivePerformance> findWithPerformanceById(@Param("id") Long id);
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/repository/LivePerformanceRepository.java
git commit -m "feat: 라이브 공연 리포지토리 추가 (BAC-89)"
git push
```

---

## Task 4: `LivePerformanceUpdateRequest` DTO

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/dto/LivePerformanceUpdateRequest.java`

`performanceId`는 `null` 허용(= 해제 의미)이므로 `@NotNull`을 붙이지 않는다. 별도 테스트는 Task 7에서 검증된다.

- [ ] **Step 1: DTO 작성**

`PerformanceCreateServiceRequest`(record)와 같은 record 스타일.

```java
package com.likelion.yonsei.daedongje.domain.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "라이브 공연 지정/해제 요청")
public record LivePerformanceUpdateRequest(

        @Schema(description = "라이브로 지정할 공연 ID. null 이면 라이브 해제", example = "12")
        Long performanceId
) {
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/dto/LivePerformanceUpdateRequest.java
git commit -m "feat: 라이브 공연 지정 요청 DTO 추가 (BAC-89)"
git push
```

---

## Task 5: `LivePerformanceService`

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/service/LivePerformanceService.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/performance/service/LivePerformanceServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`PerformanceServiceTest`와 같은 Mockito 단위 테스트 스타일(`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `ReflectionTestUtils`).

```java
package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivePerformanceServiceTest {

    @Mock
    private LivePerformanceRepository livePerformanceRepository;

    @Mock
    private PerformanceRepository performanceRepository;

    @InjectMocks
    private LivePerformanceService livePerformanceService;

    @Test
    void getLivePerformance_returns_null_when_row_is_absent() {
        when(livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.empty());

        assertThat(livePerformanceService.getLivePerformance()).isNull();
    }

    @Test
    void getLivePerformance_returns_null_when_no_performance_is_pinned() {
        when(livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.of(LivePerformance.singleton()));

        assertThat(livePerformanceService.getLivePerformance()).isNull();
    }

    @Test
    void getLivePerformance_returns_pinned_performance() {
        LivePerformance livePerformance = LivePerformance.singleton();
        livePerformance.updatePerformance(performance(12L));
        when(livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.of(livePerformance));

        PerformanceCurrentResponse response = livePerformanceService.getLivePerformance();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(12L);
    }

    @Test
    void updateLivePerformance_pins_performance_and_creates_row_when_absent() {
        Performance performance = performance(12L);
        when(performanceRepository.findById(12L)).thenReturn(Optional.of(performance));
        when(livePerformanceRepository.findById(LivePerformance.SINGLETON_ID)).thenReturn(Optional.empty());
        when(livePerformanceRepository.save(any(LivePerformance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerformanceCurrentResponse response = livePerformanceService.updateLivePerformance(12L);

        ArgumentCaptor<LivePerformance> captor = ArgumentCaptor.forClass(LivePerformance.class);
        verify(livePerformanceRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(LivePerformance.SINGLETON_ID);
        assertThat(captor.getValue().getPerformance()).isEqualTo(performance);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(12L);
    }

    @Test
    void updateLivePerformance_clears_pointer_when_performance_id_is_null() {
        when(livePerformanceRepository.findById(LivePerformance.SINGLETON_ID))
                .thenReturn(Optional.of(LivePerformance.singleton()));
        when(livePerformanceRepository.save(any(LivePerformance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerformanceCurrentResponse response = livePerformanceService.updateLivePerformance(null);

        ArgumentCaptor<LivePerformance> captor = ArgumentCaptor.forClass(LivePerformance.class);
        verify(livePerformanceRepository).save(captor.capture());
        assertThat(captor.getValue().getPerformance()).isNull();
        assertThat(response).isNull();
    }

    @Test
    void updateLivePerformance_throws_when_performance_does_not_exist() {
        when(performanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> livePerformanceService.updateLivePerformance(99L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    private Performance performance(Long id) {
        Performance performance = (Performance) ReflectionTestUtils.invokeConstructor(Performance.class);
        ReflectionTestUtils.setField(performance, "id", id);
        ReflectionTestUtils.setField(performance, "performanceName", "Main Stage");
        return performance;
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.service.LivePerformanceServiceTest"`
Expected: 컴파일 실패 — `LivePerformanceService` 심볼을 찾을 수 없음

- [ ] **Step 3: 서비스 구현**

조회는 fetch join 쿼리, 갱신은 싱글톤 행 upsert(`findById(...).orElseGet(LivePerformance::singleton)`)로 시드 행 유무와 무관하게 동작한다. `performanceStatus`는 일절 건드리지 않는다.

```java
package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진이 수동 지정하는 '라이브 공연' 포인터의 조회·갱신을 담당한다.
 *
 * <p>라이브 포인터는 {@code performanceStatus} 와 직교한다 — 지정/해제는 공연 상태를 바꾸지 않는다.
 * 핀된 공연이 {@code HIDDEN} 상태여도 {@link #getLivePerformance()} 는 그대로 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LivePerformanceService {

    private final LivePerformanceRepository livePerformanceRepository;
    private final PerformanceRepository performanceRepository;

    /** 현재 라이브 공연. 미지정(행 없음 또는 performance 가 null)이면 {@code null} 을 반환한다. */
    public PerformanceCurrentResponse getLivePerformance() {
        return livePerformanceRepository.findWithPerformanceById(LivePerformance.SINGLETON_ID)
                .map(LivePerformance::getPerformance)
                .map(PerformanceCurrentResponse::from)
                .orElse(null);
    }

    /**
     * 라이브 공연을 지정/교체하거나({@code performanceId} 지정) 해제한다({@code performanceId == null}).
     * 시드 행이 없으면 싱글톤 행을 새로 만든다.
     */
    @Transactional
    public PerformanceCurrentResponse updateLivePerformance(Long performanceId) {
        Performance performance = resolvePerformance(performanceId);

        LivePerformance livePerformance = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID)
                .orElseGet(LivePerformance::singleton);
        livePerformance.updatePerformance(performance);
        livePerformanceRepository.save(livePerformance);

        return performance == null ? null : PerformanceCurrentResponse.from(performance);
    }

    private Performance resolvePerformance(Long performanceId) {
        if (performanceId == null) {
            return null;
        }
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.service.LivePerformanceServiceTest"`
Expected: `BUILD SUCCESSFUL`, 6 tests passed

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/service/LivePerformanceService.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/performance/service/LivePerformanceServiceTest.java
git commit -m "feat: 라이브 공연 조회/지정 서비스 추가 (BAC-89)"
git push
```

---

## Task 6: `GET /api/performances/live` — 공개 조회 엔드포인트

**Files:**
- Modify: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LivePerformanceControllerTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`PerformanceReadControllerTest`의 통합 테스트 스타일(`@SpringBootTest`, `@ActiveProfiles("test")`, `@AutoConfigureMockMvc`). 라이브 포인터는 `LivePerformanceRepository`로 직접 세팅한다.

```java
package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LivePerformanceControllerTest {

    private static final String LIVE_URL = "/api/performances/live";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private MapLocationRepository mapLocationRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private LivePerformanceRepository livePerformanceRepository;

    private int adminSequence;

    @BeforeEach
    void setUp() {
        livePerformanceRepository.deleteAll();
        performanceRepository.deleteAll();
        mapLocationRepository.deleteAll();
        adminUserRepository.deleteAll();
        adminSequence = 0;
    }

    @Test
    void getLivePerformance_returns_null_when_no_row_exists() throws Exception {
        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getLivePerformance_returns_null_when_pointer_is_not_pinned() throws Exception {
        livePerformanceRepository.save(LivePerformance.singleton());

        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getLivePerformance_returns_pinned_performance() throws Exception {
        MapLocation location = mapLocationRepository.save(mapLocation("Outdoor Stage"));
        Performance performance = visiblePerformance("Live Stage", location);
        pin(performance);

        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Live Stage"))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("20:00:00"))
                .andExpect(jsonPath("$.data.performanceStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Outdoor Stage"));
    }

    @Test
    void getLivePerformance_returns_hidden_performance_as_is() throws Exception {
        // 핀된 공연이 HIDDEN 이어도 운영진이 명시 지정한 값이므로 그대로 반환한다.
        Performance hidden = performanceRepository.save(Performance.create(adminUser(), "Hidden Stage"));
        pin(hidden);

        mockMvc.perform(get(LIVE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(hidden.getId()))
                .andExpect(jsonPath("$.data.performanceStatus").value("HIDDEN"));
    }

    private void pin(Performance performance) {
        LivePerformance livePerformance = LivePerformance.singleton();
        livePerformance.updatePerformance(performance);
        livePerformanceRepository.save(livePerformance);
    }

    private Performance visiblePerformance(String name, MapLocation location) {
        Performance performance = Performance.create(adminUser(), name);
        performance.updateBasicInfo(
                location,
                name,
                name + " description",
                1,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                PerformanceCategory.ARTIST,
                "Lineup A",
                PerformanceStatus.ONGOING
        );
        return performanceRepository.save(performance);
    }

    private AdminUser adminUser() {
        adminSequence++;
        return adminUserRepository.save(AdminUser.create(
                "performer-" + adminSequence,
                "password-hash",
                "Performance Team " + adminSequence,
                AdminRole.PERFORMER,
                "Representative",
                "010-0000-%04d".formatted(adminSequence),
                null
        ));
    }

    private MapLocation mapLocation(String locationName) {
        return MapLocation.create(
                locationName,
                "A",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                MapLocationType.STAGE,
                1,
                MapDisplayStatus.VISIBLE
        );
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.LivePerformanceControllerTest"`
Expected: 4개 테스트 실패 — `GET /api/performances/live` 가 404 또는 매핑 없음

- [ ] **Step 3: `PerformanceReadController`에 엔드포인트 추가**

`livePerformanceService` 의존성과 `GET /live` 메서드를 추가한다. import 2줄(`LivePerformanceService`)도 추가한다. 다른 메서드는 그대로 둔다.

`PerformanceReadController.java`의 import 블록에 추가:

```java
import com.likelion.yonsei.daedongje.domain.performance.service.LivePerformanceService;
```

필드 선언을 다음으로 교체:

```java
    private final PerformanceReadService performanceReadService;
    private final LivePerformanceService livePerformanceService;
```

`/timetable` 메서드와 `/{id}` 메서드 사이에 다음 메서드를 추가:

```java
    @Operation(summary = "라이브 공연 조회", description = "운영진이 수동 지정한 현재 라이브 공연을 조회합니다. 미지정 시 data는 null입니다.")
    @GetMapping("/live")
    public ApiResponse<PerformanceCurrentResponse> getLivePerformance() {
        return ApiResponse.success(livePerformanceService.getLivePerformance());
    }
```

> 주의: `/live`는 반드시 `/{id}` **앞에** 선언한다 — Spring은 정적 경로(`/live`)를 변수 경로(`/{id}`)보다 우선 매칭하므로 순서가 동작에 영향을 주진 않지만, 가독성을 위해 다른 정적 경로(`/current`, `/timetable`)와 함께 둔다.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.LivePerformanceControllerTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/PerformanceReadController.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LivePerformanceControllerTest.java
git commit -m "feat: 라이브 공연 공개 조회 API 추가 (BAC-89)"
git push
```

---

## Task 7: `PUT /api/admin/performances/live` — SUPER 전용 지정/해제 엔드포인트

**Files:**
- Create: `src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/LivePerformanceAdminController.java`
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LivePerformanceAdminControllerTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`PerformanceAdminControllerTest`의 인증 테스트 스타일(`@MockBean AdminAuthContextService`, `AdminSessionUser.from(...)`)을 따른다. 권한 거부 시 에러코드는 `A-009`다.

```java
package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.repository.LivePerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LivePerformanceAdminControllerTest {

    private static final String LIVE_URL = "/api/admin/performances/live";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private LivePerformanceRepository livePerformanceRepository;

    @MockBean
    private AdminAuthContextService adminAuthContextService;

    private int adminSequence;

    @BeforeEach
    void setUp() {
        livePerformanceRepository.deleteAll();
        performanceRepository.deleteAll();
        adminUserRepository.deleteAll();
        Mockito.reset(adminAuthContextService);
        adminSequence = 0;

        AdminUser superAdmin = adminUserRepository.save(adminUser("super", AdminRole.SUPER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(superAdmin));
    }

    @Test
    void putLive_pins_performance_and_persists() throws Exception {
        Performance performance = performanceRepository.save(performerPerformance("Live Stage"));

        String body = """
                { "performanceId": %d }
                """.formatted(performance.getId());

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Live Stage"));

        LivePerformance saved = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID).orElseThrow();
        assertThat(saved.getPerformance().getId()).isEqualTo(performance.getId());
    }

    @Test
    void putLive_replaces_existing_pinned_performance() throws Exception {
        Performance first = performanceRepository.save(performerPerformance("First Stage"));
        Performance second = performanceRepository.save(performerPerformance("Second Stage"));
        LivePerformance pointer = LivePerformance.singleton();
        pointer.updatePerformance(first);
        livePerformanceRepository.save(pointer);

        String body = """
                { "performanceId": %d }
                """.formatted(second.getId());

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(second.getId()));

        LivePerformance saved = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID).orElseThrow();
        assertThat(saved.getPerformance().getId()).isEqualTo(second.getId());
    }

    @Test
    void putLive_clears_pointer_when_performance_id_is_null() throws Exception {
        Performance performance = performanceRepository.save(performerPerformance("Live Stage"));
        LivePerformance pointer = LivePerformance.singleton();
        pointer.updatePerformance(performance);
        livePerformanceRepository.save(pointer);

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"performanceId\": null }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        LivePerformance saved = livePerformanceRepository.findById(LivePerformance.SINGLETON_ID).orElseThrow();
        assertThat(saved.getPerformance()).isNull();
    }

    @Test
    void putLive_returns_not_found_when_performance_does_not_exist() throws Exception {
        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"performanceId\": 999999 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void putLive_is_rejected_for_non_super_admin() throws Exception {
        AdminUser performerAdmin = adminUserRepository.save(adminUser("performer-admin", AdminRole.PERFORMER));
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(AdminSessionUser.from(performerAdmin));

        mockMvc.perform(put(LIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"performanceId\": null }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    private Performance performerPerformance(String name) {
        return Performance.create(adminUser("performer", AdminRole.PERFORMER), name);
    }

    private AdminUser adminUser(String loginIdPrefix, AdminRole role) {
        adminSequence++;
        return AdminUser.create(
                loginIdPrefix + "-" + adminSequence,
                "password-hash",
                "Team " + adminSequence,
                role,
                "Representative",
                "010-0000-%04d".formatted(adminSequence),
                null
        );
    }
}
```

> 참고: `performerPerformance(...)`는 `adminUser(...)`를 호출해 PERFORMER 어드민을 만들어 `Performance`에 연결한다(`Performance.create`가 PERFORMER 역할을 요구함). 단, `Performance` 저장 전에 그 어드민이 영속화되어야 한다 — `Performance.create`에 넘기는 `AdminUser`를 먼저 저장하도록 헬퍼를 다음과 같이 수정한다(Step 1 작성 시 이 버전 사용):
>
> ```java
>     private Performance performerPerformance(String name) {
>         AdminUser performer = adminUserRepository.save(adminUser("performer", AdminRole.PERFORMER));
>         return Performance.create(performer, name);
>     }
> ```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.LivePerformanceAdminControllerTest"`
Expected: 테스트 실패 — `PUT /api/admin/performances/live` 매핑 없음(404)

- [ ] **Step 3: `LivePerformanceAdminController` 구현**

`PerformanceAdminController`(타입 레벨 `@RequireAdminRole`)와 `PerformanceSetlistAdminController`의 스타일을 따른다. SUPER 전용이므로 타입 레벨에 `@RequireAdminRole(AdminRole.SUPER)`를 둔다.

```java
package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.performance.dto.LivePerformanceUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.service.LivePerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공연 라이브 어드민", description = "운영진(SUPER)이 라이브 공연을 지정/교체/해제하는 API")
@RestController
@RequestMapping("/api/admin/performances")
@RequireAdminRole(AdminRole.SUPER)
@RequiredArgsConstructor
public class LivePerformanceAdminController {

    private final LivePerformanceService livePerformanceService;

    @Operation(
            summary = "라이브 공연 지정/해제",
            description = "현재 라이브 공연을 지정·교체하거나, performanceId를 null로 보내 해제합니다. 해제 시 data는 null입니다."
    )
    @PutMapping("/live")
    public ApiResponse<PerformanceCurrentResponse> updateLivePerformance(
            @RequestBody LivePerformanceUpdateRequest request
    ) {
        return ApiResponse.success(livePerformanceService.updateLivePerformance(request.performanceId()));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.performance.controller.LivePerformanceAdminControllerTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/performance/controller/LivePerformanceAdminController.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/performance/controller/LivePerformanceAdminControllerTest.java
git commit -m "feat: 라이브 공연 지정/해제 어드민 API 추가 (BAC-89)"
git push
```

---

## Task 8: 전체 회귀 검증

**Files:** 없음 (검증 전용)

- [ ] **Step 1: 전체 테스트 실행**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL` — 신규 테스트 17개 포함 전체 통과, 기존 테스트 회귀 없음

- [ ] **Step 2: 실패 시 대응**

기존 테스트가 깨졌다면 — 특히 `PerformanceReadControllerTest`의 OpenAPI 검증(`openApi_exposes_user_performance_read_apis_without_out_of_scope_posts`) — 실패 메시지를 확인한다. `/live`는 `post`를 추가하지 않으므로 해당 테스트는 영향받지 않아야 한다. 깨졌다면 근본 원인을 파악해 수정하고 Step 1로 돌아간다.

- [ ] **Step 3: PR 생성**

CLAUDE.md의 PR 작성 규칙을 따른다 — `.github/PULL_REQUEST_TEMPLATE.md` 구조 준수, 관련 이슈 `closes #173`, Function ID `P-01`, 본문 한국어, 자동 서명 푸터 금지. PR 생성 전 사용자에게 이슈 번호(#173)와 Function ID(P-01)를 재확인한다.

---

## Self-Review 결과

**Spec coverage:**
- `GET /api/performances/live` 공개 조회 → Task 6 ✓
- `PUT /api/admin/performances/live` SUPER 전용 → Task 7 ✓
- 단일 행 테이블 + 시드 → Task 1 ✓
- 엔티티/리포지토리/DTO/서비스 → Task 2~5 ✓
- 미지정 시 `data: null` → Task 5·6 테스트 ✓
- 존재하지 않는 ID → 404 P-006 → Task 5·7 테스트 ✓
- `performanceStatus` 불변(직교) → 서비스가 상태를 건드리지 않음, 별도 변경 없음 ✓
- HIDDEN 공연 그대로 반환 → Task 6 테스트 `getLivePerformance_returns_hidden_performance_as_is` ✓
- `updated_by` 생략 → 테이블/엔티티에 미포함 ✓
- 응답 DTO `PerformanceCurrentResponse` 재사용 → Task 5~7 ✓

**Placeholder scan:** TODO/TBD 없음. 모든 step에 실제 코드/명령 포함.

**Type consistency:** `LivePerformance.SINGLETON_ID`, `singleton()`, `updatePerformance(Performance)`, `LivePerformanceService.getLivePerformance()`, `updateLivePerformance(Long)`, `LivePerformanceRepository.findWithPerformanceById(Long)`, `LivePerformanceUpdateRequest.performanceId()` — Task 2~7 전반에서 동일하게 사용됨. 일관성 확인 완료.

**테스트 환경 주의:** 테스트(H2)는 Flyway 비활성·`create-drop`이라 시드 행이 없다. 서비스의 `updateLivePerformance`가 `orElseGet(LivePerformance::singleton)`로 upsert 하므로 시드 유무와 무관하게 동작한다. Task 6 GET 테스트는 `LivePerformanceRepository`로 행을 직접 세팅한다.
