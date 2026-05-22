# 무대별 실시간 공연 조회 API 설계

- **Linear**: BAC-114
- **GitHub Issue**: #230
- **Function ID**: P-01 (라이브/현재 공연 조회 묶음)
- **브랜치**: `feature/bac-114-p-01-per-stage-live-performance-read-api-user-artist`
- **작성일**: 2026-05-23

## 배경 / 문제

사용자에게 "지금 각 무대에서 무슨 공연을 하는지"를 무대별로 보여줘야 한다. 현재 공연 API는 두 종류가 있는데 둘 다 **전역 1건**만 반환하고 무대(`MapLocation`) 개념이 없다.

- `GET /api/performances/current` — `performance_status == ONGOING`인 공연 중 가장 이른 1건. 이름은 "현재 진행 중"이지만 **시간 기반이 아니라 상태(ONGOING) 기반**이고, 상태를 자동 전이시키는 스케줄러가 프로젝트에 **없다**(운영진이 손으로 status를 바꿔야만 뜸).
- `GET /api/performances/live` — 운영진이 수동 지정한 `LivePerformance` 싱글톤 핀 1건. status와 직교(HIDDEN이어도 노출).

아티스트 공연은 타임라인(시작/종료 시간)을 제공하지 않아 수동 지정이 필수이고, 동아리 공연은 타임라인이 있어 시간 기반 자동 판정이 가능하다. 이 둘을 무대별로 한 번에 내려주는 하이브리드 조회가 필요하다.

## 목표 / 비목표

**목표**
- 무대별 "지금 진행 중인 공연"을 한 번의 호출로 반환하는 사용자용 읽기 API.
- 아티스트 메인 무대: 기존 수동 핀 그대로 사용. 동아리 무대: 시간 기반 자동 판정.

**비목표 (이번 범위 아님)**
- 공연 status를 시간 따라 자동 전이시키는 스케줄러 도입 (읽기 시점 계산으로 대체).
- 동아리 무대의 수동 override / 무대별 핀 확장 (아티스트=단일 핀, 동아리=자동으로 분리).
- 관리자 화면·통계에서 "진행 중"을 DB 컬럼으로 집계 (불필요로 확인됨).
- DB 스키마 변경 (읽기 전용 기능).

## 핵심 결정

1. **읽기 시점 계산 (Read-time computed)**: 조회마다 그 자리에서 "지금 진행 중"을 계산한다. `performance_status`는 건드리지 않으며(노출/HIDDEN 필터 용도로만 유지), 스케줄러를 도입하지 않는다.
2. **카테고리가 자동/수동 갈림축**: `ARTIST` = 수동 핀, `CLUB` = 시간 기반 자동.
3. **무대 분리 가정(B)**: 아티스트는 메인 무대 1개(수동), 동아리는 무대 여러 개(자동). 한 무대에서 자동·수동이 동시에 잡히지 않는다 → 무대 내 우선순위 규칙 불필요.
4. **수동은 단일 핀**: 기존 `LivePerformance` 싱글톤(`SINGLETON_ID = 1`)을 그대로 사용. 무대별 핀으로 확장하지 않는다.
5. **시간 기준은 Asia/Seoul**: 일차는 `FestivalDayService.getCurrentFestivalDay()`(축제 기간 밖이면 2/4로 클램핑하는 기존 동작 그대로), 시각은 주입된 `Clock` 기준 `LocalTime.now(clock)`.

## API 설계

### 신규 — `GET /api/performances/live-stages` (사용자용)

무대별 현재 공연을 배열로 반환한다. **지금 공연 중인 무대만** 포함(공연 없는 무대는 생략).

응답 예시:
```jsonc
{
  "success": true,
  "data": [
    { "source": "MANUAL", "performance": { "id": 12, "performanceName": "...", "locationId": 1, "locationName": "노천극장", "startTime": null, "endTime": null, "performanceCategory": "ARTIST", ... } },
    { "source": "AUTO",   "performance": { "id": 31, "performanceName": "...", "locationId": 5, "locationName": "백양로 무대", "startTime": "18:00", "endTime": "18:40", "performanceCategory": "CLUB", ... } }
  ]
}
```

- 무대 식별은 `performance.locationId` / `performance.locationName`로 한다. `PerformanceCurrentResponse`가 이미 location 정보를 담고 있어 별도 stage 필드를 두지 않는다.
- `source`: `MANUAL`(수동 핀) | `AUTO`(시간 자동).
- 배열 순서: MANUAL 먼저, 그다음 AUTO를 무대 `displayOrder → id` 순.

### 기존 엔드포인트 처리

- `PUT /api/admin/performances/live` (운영진 핀 지정/해제): **변경 없음**. 아티스트 수동 핀 입력 경로로 계속 사용.
- `GET /api/performances/live` (싱글톤 핀 조회): **유지**(하위호환). 신규 엔드포인트가 메인 무대를 포함하므로 사실상 대체되지만 당장 제거하지 않는다.
- `GET /api/performances/current`: **유지하되 Swagger에 deprecated 표기**. 프론트가 `/live-stages`로 이전 완료되면 후속 이슈에서 제거.

## 계산 로직 (`LiveStageService`)

입력: `today = festivalDayService.getCurrentFestivalDay()`, `now = LocalTime.now(clock)`.

**수동(MANUAL)**
- `livePerformanceService.getLivePerformance()` 반환값(`PerformanceCurrentResponse` 또는 null)을 사용.
- null이 아니면 `{ MANUAL, response }` 1건. 핀은 status 무관 노출(기존 동작 유지). 핀 공연의 location이 null이어도 엔트리는 포함(운영진이 명시 지정했으므로).

**자동(AUTO)**
1. 리포지토리에서 `category == CLUB` AND `performance_date == today` AND `status NOT IN (HIDDEN, CANCELED, ENDED)` 인 공연을 `location` fetch-join으로 조회.
2. 서비스에서 필터: `startTime != null && endTime != null && endTime.isAfter(startTime)` AND `!startTime.isAfter(now) && endTime.isAfter(now)` (즉 `start <= now < end`) AND `location != null`.
3. 무대(`location.id`)별 그룹핑 후, 무대당 `startTime → id` 가장 이른 1건만 선택.
4. 각 공연을 `PerformanceCurrentResponse.from(...)` → `{ AUTO, response }`.

**조립**
- MANUAL 엔트리 + AUTO 엔트리들.
- **중복 제거**: AUTO 엔트리의 `locationId`가 MANUAL 엔트리의 `locationId`와 같으면 MANUAL 우선(해당 AUTO 생략). 무대 분리 가정상 발생하지 않지만 방어적으로 처리.

상태 제외 근거: `HIDDEN`=미공개(노출 금지), `CANCELED`=취소, `ENDED`=운영진이 조기 종료 명시. 그 외(`SCHEDULED`/`ONGOING`)는 시간으로만 판정 — 시작 전엔 시간 필터에서, 종료 후엔 `end > now` 실패로 자동으로 빠진다.

## 엣지케이스

| 케이스 | 처리 |
|---|---|
| `now == start` | 포함 (`start <= now`) |
| `now == end` | 제외 (`now < end`, 반열린 구간) — 백투백 공연 경계 중복 방지 |
| CLUB인데 start/end 미입력 | 자동 제외 (타임라인 없는 건 ARTIST 수동 영역) |
| `end <= start` (자정 넘김/오타) | 제외 + 로그. **자정 넘김 공연 없음** 가정 |
| CLUB인데 location null | 자동 제외 (무대 귀속 불가) |
| HIDDEN/CANCELED/ENDED CLUB | 자동 제외 |
| 수동 핀이 HIDDEN 공연 | **노출**(기존 동작 유지, 의도된 비대칭) |
| 수동 핀 미지정 | MANUAL 엔트리 없음 |
| 같은 무대 CLUB 시간 겹침 | 무대당 1건(`start→id` 최이른) |
| 진행 중 아무것도 없음 | 빈 배열 `[]` |
| 축제 기간 밖 | `FestivalDayService` 클램핑(2/4) 동작 그대로 |

## 컴포넌트 / 파일

**신규**
- `domain/performance/dto/LiveStageResponse.java` — `{ LiveStageSource source, PerformanceCurrentResponse performance }` + static factory.
- `domain/performance/entity/LiveStageSource.java` — enum `{ MANUAL, AUTO }`.
- `domain/performance/service/LiveStageService.java` — 의존: `LivePerformanceService`, `PerformanceRepository`, `FestivalDayService`, `Clock`. 단일 책임: 무대별 라이브 조립.
- `config/ClockConfig.java` — `@Bean Clock clock()` → `Clock.system(ZoneId.of("Asia/Seoul"))`.

**수정**
- `domain/performance/repository/PerformanceRepository.java` — `@EntityGraph(attributePaths = "location") List<Performance> findAllByPerformanceCategoryAndPerformanceDateAndPerformanceStatusNotIn(PerformanceCategory category, Integer performanceDate, Collection<PerformanceStatus> statuses)` 추가.
- `domain/performance/controller/PerformanceReadController.java` — `GET /live-stages` 추가, `getCurrentPerformance`에 deprecated 설명.

**변경 없음**: DB 마이그레이션(스키마 변경 0), `FestivalDayService`(공유 서비스라 미수정 — 테스트에선 `@MockBean`으로 고정).

## 테스트

`@SpringBootTest` + H2 슬라이스(기존 `NoticeControllerTest` 패턴). 시간은 `Clock.fixed(...)` 빈으로, 일차는 `@MockBean FestivalDayService`로 고정해 결정적으로 검증.

- 2개 무대에서 CLUB 진행 중 → AUTO 2건, `displayOrder` 순.
- CLUB 시작 전 / 종료 후 → 제외.
- 경계: `now==start` 포함, `now==end` 제외.
- HIDDEN / CANCELED / ENDED CLUB → 제외.
- CLUB start/end null → 제외.
- 같은 무대 시간 겹침 → 1건(최이른).
- 수동 핀 set → MANUAL 1건 / 미지정 → MANUAL 없음.
- 수동 핀과 자동이 같은 무대 → MANUAL만.
- 아무것도 없음 → 빈 배열.

`Clock` 주입으로 `LocalTime.now(clock)`가 테스트에서 고정되므로 시간 의존 분기를 모두 커버한다.
