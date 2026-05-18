# 공연 라이브(현재 공연) 수동 지정 API — 설계 문서

- 작성일: 2026-05-19
- Linear: BAC-89 / GitHub 이슈: #173
- Function ID: P-01
- 브랜치: `feature/bac-89-p-01-performance-live-pin-api`

## 1. 배경 / 목적

운영진(SUPER 어드민)이 무대에서 지금 공연 중인 팀 1건을 수동으로 지정·교체·해제하고,
그 값을 어드민 ON AIR 배너와 관객용 앱이 읽는다.

아티스트 공연은 정해진 타임라인을 제공받을 수 없어 자동 파생이 불가능하므로,
운영진이 **'노천극장' 화면에서만** 손으로 핀(pin)하는 용도다. 무대가 노천극장 하나뿐이라
전역 단일 포인터 하나로 충분하다.

기존 `GET /api/performances/current`(상태 `ONGOING` 기반 자동 파생)와는 **별개**다.
라이브 포인터는 운영진이 손으로 지정하는 단일 포인터이고, 둘은 독립적으로 공존한다.

## 2. 엔드포인트

| 메서드·경로 | 권한 | 설명 |
|---|---|---|
| `GET /api/performances/live` | 공개 (인증 불필요) | 현재 지정된 라이브 공연 1건. 미지정 시 `data: null` |
| `PUT /api/admin/performances/live` | `@RequireAdminRole(SUPER)` | 라이브 공연 지정/교체/해제 |

`GET`은 `PerformanceReadController`(공개), `PUT`은 어드민 경로 — `/current`·`/timetable`이
공개인 것과 동일 패턴이다.

## 3. 요청 / 응답

### GET /api/performances/live

- 응답: `ApiResponse<PerformanceCurrentResponse>` — 기존 `/current`와 동일 DTO 재사용.
- 미지정 시: `{ "success": true, "data": null }`

### PUT /api/admin/performances/live

- 요청 바디:
  ```jsonc
  { "performanceId": 12 }   // 지정/교체
  { "performanceId": null } // 해제
  ```
- 응답: `ApiResponse<PerformanceCurrentResponse>` — 적용 결과 (해제 시 `data: null`).

## 4. 데이터 모델

전역 단일 값이므로 **단일 행 포인터 테이블**로 둔다. `Performance`에 boolean 플래그를
두면 "동시에 여러 개 true" 사고가 나므로 지양한다.

### Flyway 마이그레이션 — `V25__create_live_performance_table.sql`

```sql
CREATE TABLE live_performance (
    id             BIGINT PRIMARY KEY,          -- 고정값 1 (AUTO_INCREMENT 아님)
    performance_id BIGINT NULL,                 -- NULL = 라이브 미지정
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_live_performance_performance
        FOREIGN KEY (performance_id) REFERENCES performances(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO live_performance (id, performance_id, created_at, updated_at)
VALUES (1, NULL, NOW(6), NOW(6));
```

- 행 하나를 시드로 미리 넣어두고, 서비스는 항상 그 행만 읽고 쓴다.
- `performance_id` FK로 존재하지 않는 공연을 가리킬 수 없도록 보장한다.
- 행이 하나뿐이라 "동시 라이브" 가 구조적으로 불가능하다.
- `updated_by`(지정한 운영자 추적)는 운영 책임 추적이 실제로 필요할 때까지 생략한다.

> 결정 근거: 범용 key-value 설정 테이블도 후보였으나, FK 무결성이 없고 현 레포에
> 그런 테이블이 없어 미리 범용 메커니즘을 만드는 것은 YAGNI에 어긋나므로 제외.

## 5. 컴포넌트

### 5.1 엔티티 — `LivePerformance extends BaseEntity`

- `@Id Long id` — 생성 전략 없음, 항상 `1`.
- `@ManyToOne(fetch = LAZY) Performance performance` — `performance_id` FK, nullable.
- 메서드 `updatePerformance(Performance performance)` — `null`을 넣으면 해제.
- 상수 `LIVE_POINTER_ID = 1L`.
- `created_at`/`updated_at`은 `BaseEntity` + JPA Auditing이 관리. 시드 행이 이미
  존재하므로 매 갱신 시 `updated_at`만 자동 갱신된다.

### 5.2 리포지토리 — `LivePerformanceRepository extends JpaRepository<LivePerformance, Long>`

응답에 `locationName`이 필요하므로 N+1 방지용 fetch join 쿼리 추가:

```java
@Query("SELECT lp FROM LivePerformance lp " +
       "LEFT JOIN FETCH lp.performance p LEFT JOIN FETCH p.location " +
       "WHERE lp.id = :id")
Optional<LivePerformance> findWithPerformanceById(@Param("id") Long id);
```

### 5.3 DTO

- **응답**: 기존 `PerformanceCurrentResponse` 재사용 (`from(Performance)` 팩토리 존재).
- **요청**: 신규 `LivePerformanceUpdateRequest { Long performanceId; }`
  — `@NotNull` 없음 (`null` = 해제).

### 5.4 서비스 — 신규 `LivePerformanceService`

조회·갱신을 한 서비스에 응집한다 (라이브 핀 로직 한 곳).

- `getLivePerformance()`
  - 싱글톤 행(`LIVE_POINTER_ID`)을 `findWithPerformanceById`로 조회.
  - `performance == null`이면 `null` 반환, 아니면 `PerformanceCurrentResponse.from(performance)`.
  - **공연이 `HIDDEN` 상태여도 핀 값을 그대로 반환** — SUPER가 명시적으로 지정한 값이 우선.
- `updateLivePerformance(Long performanceId)`
  - `performanceId != null`: `performanceRepository.findById`로 조회, 없으면
    `BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND)` (P-006, 기존 코드 재사용).
  - `performanceId == null`: 해제.
  - 싱글톤 행의 `performance`를 갱신하고 결과를 응답으로 반환.
- `performanceStatus`는 일절 건드리지 않는다 (라이브 포인터와 상태는 직교).

### 5.5 컨트롤러

- **`PerformanceReadController`에 메서드 추가** — `GET /live`, 공개.
  `LivePerformanceService.getLivePerformance()` 호출.
- **신규 `LivePerformanceAdminController`** — `@RequestMapping("/api/admin/performances")`,
  타입 레벨 `@RequireAdminRole(AdminRole.SUPER)`, `PUT /live` 하나.
  - 기존 `PerformanceAdminController`는 `@RequireAdminRole({PERFORMER, SUPER})`라
    SUPER 전용 엔드포인트는 별도 컨트롤러로 분리한다.

## 6. 검증 / 에러

- 존재하지 않는 `performanceId` → `404` + `P-006 PERFORMANCE_NOT_FOUND` (기존 에러코드 재사용, 신규 불필요).
- `performanceId: null` → 정상 해제.
- 단일 행 포인터라 "한 번에 한 공연" 제약은 구조적으로 보장된다.
- SUPER 전용 기능이므로 그 외 추가 검증은 최소화한다.

## 7. 결정 사항 요약

| 결정 | 선택 | 근거 |
|---|---|---|
| 라이브 지정이 `performanceStatus`를 바꾸나? | **아니오 (직교)** | 두 개념 분리, 부작용·상태 복원 문제 회피 |
| 데이터 모델 | **전용 단일 행 테이블** | FK 무결성, 동시 라이브 구조적 차단 |
| `updated_by` 컬럼 | **생략** | 운영 책임 추적이 실제 필요할 때까지 미루기 |
| `HIDDEN` 공연이 핀된 경우 `GET /live` | **핀 값 그대로 반환** | SUPER가 명시 지정한 값 우선, 검증 최소화 |

## 8. 테스트

- `LivePerformanceService` 단위 테스트: 미지정→`null`, 지정, 교체, 해제,
  존재하지 않는 ID→404, `HIDDEN` 공연 핀→그대로 반환.
- 컨트롤러 테스트: `GET /live` 공개 접근 가능, `PUT /live` SUPER 권한 강제 확인.

## 9. 프론트 영향 (참고)

- `GET /api/performances/live` — 프론트 `getLivePerformance`가 이미 이 경로를 호출하므로
  그대로 동작. 응답이 `PerformanceCurrentResponse`이므로 `getLivePerformanceReal`에서
  `.id` 추출로 1줄 조정(또는 전체 객체 활용).
- `PUT` — 프론트가 현재 `/performances/live`로 보내므로 어드민 경로
  `/admin/performances/live`로 옮기면 `setLivePerformanceReal` 경로 1줄 수정 필요.
- 백엔드 머지 후 프론트에서 함께 반영.
