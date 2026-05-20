# 예약 광클(중복 제출) 멱등 처리 설계

- 작성일: 2026-05-20
- 스코프: 로그인 없는 사용자의 예약 광클(rapid-click) 차단 — "A안"
- 범위 외: "부스당 전화번호 1건" 비즈니스 규칙("B안") — 정책 정교화 후 별도 진행

## 배경 / 문제

사용자는 로그인 없이 `POST /api/reservations/booths/{boothId}`로 예약을 생성한다.
`ReservationService.create`는 진입 시 `boothRepository.findByIdWithLock`로 부스 행에
비관적 쓰기 락을 걸어 `reservationNumber` 채번을 직렬화한다.

이 락은 요청을 **직렬화**할 뿐 **중복 제거(dedup)**는 하지 않는다. 같은 사용자가
버튼을 광클하면 POST가 N번 발생하고, 각 요청이 락을 순서대로 잡아 각각 별개
예약(#5·#6·#7…)을 생성한다. → 한 사람이 중복 예약 N건.

## 목표

같은 `boothId + phoneNumber`로 **짧은 시간 윈도우 안에** 들어온 반복 요청을
한 건의 예약으로 수렴시킨다. 2번째 이후 요청은 신규 생성 대신 **첫 예약을 그대로
반환(멱등)**한다.

- 멱등 응답 채택: 광클은 사용자 실수이므로 에러를 보여주지 않는다. 응답은 평소와
  동일한 `201 + ReservationCreateResponse` → 프론트엔드 변경 불필요.
- 윈도우: 10초 (광클 + "안 된 줄 알고 또 누름" 커버. 10초 밖 재예약은 정상 허용).

## 접근 — 기존 부스 락 안에서 DB 조회

`create`는 이미 `findByIdWithLock`로 부스 행에 쓰기 락을 건다. 같은 부스의 모든
`create`는 직렬화되어 있으므로, 요청 #2는 #1 트랜잭션이 **커밋된 뒤에야** 임계
구역에 진입한다. 따라서 락 구간 안에서 단순 DB 조회만으로 경합 없이 멱등성을
보장할 수 있다 (Redis·폴링 불필요).

Redis 멱등 캐시 방식은 in-flight 경합을 폴링으로 따로 풀어야 하고, 예약이라는
중요 경로를 Redis 가용성에 묶는다(클릭 로그와 달리 fail-open 불가). DB 조회 방식이
더 단순하고 견고하다.

## 변경 사항

### 1. `ReservationRepository` — 멱등 조회 메서드 추가

```java
@Query("SELECT r FROM Reservation r WHERE r.booth.id = :boothId " +
       "AND r.phoneNumber = :phoneNumber AND r.status = :status " +
       "AND r.createdAt >= :since ORDER BY r.createdAt DESC")
List<Reservation> findRecentDuplicates(@Param("boothId") Long boothId,
                                       @Param("phoneNumber") String phoneNumber,
                                       @Param("status") ReservationStatus status,
                                       @Param("since") LocalDateTime since);
```

`List` 반환 후 `findFirst` 사용 — 윈도우 내 멀티매치 상황에서도 안전.

### 2. `ReservationService.create` — 멱등 분기 추가

부스 락 획득 + `isReservable` 검사 직후, 신규 생성 로직 **앞에** 삽입:

```java
LocalDateTime since = LocalDateTime.now().minus(DUPLICATE_WINDOW);
List<Reservation> recent = reservationRepository.findRecentDuplicates(
        boothId, request.phoneNumber(), ReservationStatus.PENDING, since);
if (!recent.isEmpty()) {
    Reservation existing = recent.get(0);
    long aheadOfMe = reservationRepository
            .countByBoothIdAndStatus(boothId, ReservationStatus.PENDING) - 1;
    return ReservationCreateResponse.of(existing, aheadOfMe);
}
// 이하 기존 신규 생성 로직 그대로
```

### 3. 윈도우 상수

```java
private static final Duration DUPLICATE_WINDOW = Duration.ofSeconds(10);
```

### 4. DB 인덱스 — 마이그레이션 V31

```sql
CREATE INDEX idx_reservations_dedup ON reservations (booth_id, phone_number, created_at);
```

축제 중 reservations 테이블이 수천 행으로 커지므로, `create`마다 도는 멱등 조회를
range scan으로 처리하기 위한 인덱스. 컬럼 추가 없음, 인덱스만 추가.

## 동시성 정당성

`findByIdWithLock`는 `PESSIMISTIC_WRITE` 락(`SELECT ... FOR UPDATE`)으로 같은
부스의 `create`를 직렬화한다. 락은 거부가 아니라 대기다:

- 요청 #1: 부스 락 획득 → 최근 중복 없음 → 예약 생성 → 커밋 → 락 해제
- 요청 #2: (락 대기) → 락 획득 → `findRecentDuplicates`로 #1 발견 → #1 반환

#2는 #1 커밋 이후에 진입하므로 #1의 행을 반드시 본다. 경합 없음.
다른 부스 예약은 락 대상이 달라 영향받지 않는다(완전 병렬).

## 엣지 케이스 (의도된 동작)

- **10초 경계**: t=0·t=9s 클릭 → 멱등 병합. t=0·t=11s → 신규 생성(정상 재예약 허용).
- **윈도우 내 payload만 다름**(인원수 변경 후 재제출): 멱등으로 #1 반환. 10초 내
  재제출은 실수로 간주. 실제 수정은 `PATCH /api/reservations/{id}` 사용.
- **#1이 윈도우 내 취소됨**(희박): 조회가 `status = PENDING`만 보므로 취소건은
  멱등 대상에서 제외 → 재제출 시 정상적으로 신규 생성.
- **전화번호 표기 차이**: 광클은 동일 폼 재전송이라 문자열 완전일치. 정규화는
  스코프 외(불필요).

## 테스트

`ReservationServiceTest` (Mockito, repository mock) 신규 케이스:

1. 첫 생성 → `reservationRepository.save` 호출, 신규 예약 반환
2. 같은 phone+booth 재호출(윈도우 내 PENDING 존재) → 기존 예약 반환, `save` **미호출**
3. 윈도우 밖(최근 예약 없음) → 신규 생성
4. 다른 phone → 신규 생성

## 변경되지 않는 것

- API 계약: `POST /api/reservations/booths/{boothId}` → `201 + ReservationCreateResponse`
- 응답 코드, 프론트엔드, 부스 락 로직, `reservationNumber` 채번
- DTO·엔티티 컬럼

## 미해결 / 후속

- "B안" — 부스당 전화번호 1건 활성 예약 제한. 일행 분리 예약 허용 여부 등
  정책 정교화 필요. 별도 Linear 이슈로 진행.
