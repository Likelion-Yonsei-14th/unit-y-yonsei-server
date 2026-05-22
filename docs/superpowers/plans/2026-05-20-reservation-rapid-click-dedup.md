# 예약 광클 멱등 처리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 같은 `boothId + phoneNumber`로 10초 이내 반복된 예약 생성 요청을 한 건으로 수렴시키고, 2번째 이후 요청에 첫 예약을 그대로 반환(멱등)한다.

**Architecture:** `ReservationService.create`는 이미 부스 행에 비관적 락을 걸어 같은 부스의 `create`를 직렬화한다. 락 구간 안에서 `boothId + phoneNumber + status=PENDING + createdAt>=since` DB 조회를 추가해, 최근 예약이 있으면 신규 생성 없이 그 예약을 반환한다. Redis·폴링 불필요.

**Tech Stack:** Spring Boot, Spring Data JPA, JUnit 5 + Mockito, Flyway (MySQL)

**Spec:** `docs/superpowers/specs/2026-05-20-reservation-rapid-click-dedup-design.md`

**커밋 규칙:** 메시지는 한국어 + 컨벤션 접두사. 각 커밋은 만든 즉시 원격 브랜치에 푸시 (CLAUDE.md).

---

## Task 1: 멱등 조회 리포지토리 메서드 추가

**Files:**
- Modify: `src/main/java/com/likelion/yonsei/daedongje/domain/reservation/repository/ReservationRepository.java`

리포지토리 인터페이스 메서드 선언이므로 단위 테스트 없이 추가한다 (Task 3의 서비스 테스트에서 모킹되어 간접 검증되고, 컴파일로 시그니처가 검증된다).

- [ ] **Step 1: import 추가**

`ReservationRepository.java` 상단 import 블록에 추가:

```java
import java.time.LocalDateTime;
```

- [ ] **Step 2: 메서드 추가**

`ReservationRepository` 인터페이스 본문 끝, `findAllByBookerNameAndPhoneNumberWithFilter` 메서드 다음에 추가:

```java
    // 광클 멱등 처리: 같은 부스·전화번호로 since 이후 생성된 예약을 최신순으로 조회
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.booth.id = :boothId " +
            "AND r.phoneNumber = :phoneNumber " +
            "AND r.status = :status " +
            "AND r.createdAt >= :since " +
            "ORDER BY r.createdAt DESC")
    List<Reservation> findRecentDuplicates(@Param("boothId") Long boothId,
                                           @Param("phoneNumber") String phoneNumber,
                                           @Param("status") ReservationStatus status,
                                           @Param("since") LocalDateTime since);
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋 & 푸시**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/reservation/repository/ReservationRepository.java
git commit -m "feat: 예약 광클 멱등 조회 리포지토리 메서드 추가 (BAC-102)"
git push origin HEAD
```

---

## Task 2: 멱등 조회용 인덱스 마이그레이션 V31

**Files:**
- Create: `src/main/resources/db/migration/V31__add_reservation_dedup_index.sql`

현재 최신 마이그레이션은 V30이다. V31로 추가한다 (V26은 결번 — 건너뛴 번호는 정상).

- [ ] **Step 1: 마이그레이션 파일 작성**

`src/main/resources/db/migration/V31__add_reservation_dedup_index.sql` 생성:

```sql
-- 예약 광클 멱등 조회용 인덱스
-- ReservationService.create 의 findRecentDuplicates (booth_id + phone_number + created_at) 조회를
-- range scan 으로 처리하기 위한 인덱스. 컬럼 추가 없음.
CREATE INDEX idx_reservations_dedup
    ON reservations (booth_id, phone_number, created_at);
```

- [ ] **Step 2: 테스트로 마이그레이션 적용 확인**

Run: `./gradlew test --tests "*ReservationControllerTest"`
Expected: BUILD SUCCESSFUL (앱 컨텍스트 기동 시 Flyway/스키마가 V31까지 적용되며 오류 없음)

- [ ] **Step 3: 커밋 & 푸시**

```bash
git add src/main/resources/db/migration/V31__add_reservation_dedup_index.sql
git commit -m "feat: 예약 멱등 조회용 인덱스 마이그레이션 V31 추가 (BAC-102)"
git push origin HEAD
```

---

## Task 3: ReservationService.create 멱등 분기 (TDD)

**Files:**
- Create: `src/test/java/com/likelion/yonsei/daedongje/domain/reservation/service/ReservationServiceTest.java`
- Modify: `src/main/java/com/likelion/yonsei/daedongje/domain/reservation/service/ReservationService.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/com/likelion/yonsei/daedongje/domain/reservation/service/ReservationServiceTest.java` 생성:

```java
package com.likelion.yonsei.daedongje.domain.reservation.service;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateResponse;
import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("최근 중복 예약이 없으면 신규 예약을 생성한다")
    void createsNewReservationWhenNoRecentDuplicate() {
        Booth booth = booth(3L);
        when(boothRepository.findByIdWithLock(3L)).thenReturn(Optional.of(booth));
        when(reservationRepository.findRecentDuplicates(eq(3L), eq("010-1234-5678"),
                eq(ReservationStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(reservationRepository.findMaxReservationNumberByBoothId(3L)).thenReturn(Optional.of(4));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.countByBoothIdAndStatus(3L, ReservationStatus.PENDING)).thenReturn(5L);

        ReservationCreateResponse response = reservationService.create(3L, request());

        assertThat(response.getReservationNumber()).isEqualTo(5);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("최근 10초 내 같은 전화번호의 PENDING 예약이 있으면 신규 생성 없이 기존 예약을 반환한다")
    void returnsExistingReservationWhenRecentDuplicateExists() {
        Booth booth = booth(3L);
        Reservation existing = Reservation.create(booth, 7, "홍길동", "010-1234-5678", 2, null);
        ReflectionTestUtils.setField(existing, "id", 99L);
        when(boothRepository.findByIdWithLock(3L)).thenReturn(Optional.of(booth));
        when(reservationRepository.findRecentDuplicates(eq(3L), eq("010-1234-5678"),
                eq(ReservationStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));
        when(reservationRepository.countByBoothIdAndStatus(3L, ReservationStatus.PENDING)).thenReturn(5L);

        ReservationCreateResponse response = reservationService.create(3L, request());

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getReservationNumber()).isEqualTo(7);
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(reservationRepository, never()).findMaxReservationNumberByBoothId(any());
    }

    @Test
    @DisplayName("멱등 조회는 PENDING 상태와 약 10초 윈도우로 수행된다")
    void queriesDuplicatesWithPendingStatusAndTenSecondWindow() {
        Booth booth = booth(3L);
        when(boothRepository.findByIdWithLock(3L)).thenReturn(Optional.of(booth));
        when(reservationRepository.findRecentDuplicates(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservationRepository.findMaxReservationNumberByBoothId(3L)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.countByBoothIdAndStatus(3L, ReservationStatus.PENDING)).thenReturn(1L);

        LocalDateTime before = LocalDateTime.now();
        reservationService.create(3L, request());
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<ReservationStatus> statusCaptor = ArgumentCaptor.forClass(ReservationStatus.class);
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reservationRepository).findRecentDuplicates(eq(3L), eq("010-1234-5678"),
                statusCaptor.capture(), sinceCaptor.capture());

        assertThat(statusCaptor.getValue()).isEqualTo(ReservationStatus.PENDING);
        assertThat(sinceCaptor.getValue())
                .isAfterOrEqualTo(before.minusSeconds(10))
                .isBeforeOrEqualTo(after.minusSeconds(10));
    }

    private ReservationCreateRequest request() {
        return new ReservationCreateRequest("홍길동", "010-1234-5678", 2, null, true);
    }

    private Booth booth(Long id) {
        Booth booth = Booth.create(
                1L, "멋사 핫도그", "멋쟁이사자처럼", "소개",
                2, LocalTime.of(11, 0), LocalTime.of(20, 0),
                BoothSector.한글탑, 3, BoothStatus.OPEN,
                true, null, true, null,
                null, null, false, null
        );
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.reservation.service.ReservationServiceTest"`
Expected: FAIL — 멱등 분기가 없어 `create`가 `findRecentDuplicates`를 호출하지 않으므로 `returnsExistingReservationWhenRecentDuplicateExists`(save가 호출됨)와 `queriesDuplicatesWithPendingStatusAndTenSecondWindow`(verify 실패) 등이 실패한다.

- [ ] **Step 3: import 추가**

`ReservationService.java` 상단 import 블록에 추가:

```java
import java.time.Duration;
import java.time.LocalDateTime;
```

(`java.util.List`는 이미 import되어 있다.)

- [ ] **Step 4: 윈도우 상수 추가**

`ReservationService` 클래스 본문 맨 위, `private final ReservationRepository reservationRepository;` 필드 선언 **앞**에 추가:

```java
    /** 같은 전화번호의 동일 부스 예약을 광클로 간주해 멱등 처리하는 시간 윈도우. */
    private static final Duration DUPLICATE_WINDOW = Duration.ofSeconds(10);
```

- [ ] **Step 5: create 메서드에 멱등 분기 추가**

`ReservationService.create` 안에서 `if (!booth.getIsReservable()) { ... }` 블록 **다음**, `int nextNumber = ...` 줄 **앞**에 추가:

```java
        // 광클 멱등 처리: 같은 전화번호로 최근 DUPLICATE_WINDOW 안에 동일 부스 PENDING 예약이 있으면
        // 신규 생성 없이 그 예약을 그대로 반환한다. 부스 비관적 락이 create 를 직렬화하므로 경합은 없다.
        LocalDateTime since = LocalDateTime.now().minus(DUPLICATE_WINDOW);
        List<Reservation> recentDuplicates = reservationRepository.findRecentDuplicates(
                boothId, request.phoneNumber(), ReservationStatus.PENDING, since);
        if (!recentDuplicates.isEmpty()) {
            Reservation existing = recentDuplicates.get(0);
            long aheadOfExisting =
                    reservationRepository.countByBoothIdAndStatus(boothId, ReservationStatus.PENDING) - 1;
            return ReservationCreateResponse.of(existing, aheadOfExisting);
        }
```

- [ ] **Step 6: 테스트 실행 — 통과 확인**

Run: `./gradlew test --tests "com.likelion.yonsei.daedongje.domain.reservation.service.ReservationServiceTest"`
Expected: PASS — 3개 테스트 모두 통과

- [ ] **Step 7: 전체 테스트 실행 — 회귀 없음 확인**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋 & 푸시**

```bash
git add src/main/java/com/likelion/yonsei/daedongje/domain/reservation/service/ReservationService.java \
        src/test/java/com/likelion/yonsei/daedongje/domain/reservation/service/ReservationServiceTest.java
git commit -m "feat: 예약 생성 광클 중복 제출 멱등 처리 (BAC-102)"
git push origin HEAD
```

---

## 완료 후

- PR 생성은 `/ship` 또는 수동 `gh pr create`로 진행 (CLAUDE.md PR 템플릿 준수, `closes #202`, Function ID `R-02`).
- 수동 검증 권장: 운영/스테이징에서 같은 부스에 같은 전화번호로 빠르게 2회 `POST /api/reservations/booths/{id}` → 두 응답의 `id`·`reservationNumber`가 동일한지 확인.

## 후속 (범위 외)

- `findRecentDuplicates`의 윈도우 경계·상태 필터를 DB 레벨에서 검증하는 `@DataJpaTest`는 선택적 추가 검증. 현재 계획은 서비스 단위 테스트(모킹)로 멱등 분기 로직과 윈도우 인자 전달을 검증한다.
- "부스당 전화번호 1건" 비즈니스 규칙(B안)은 정책 정교화 후 별도 이슈.
