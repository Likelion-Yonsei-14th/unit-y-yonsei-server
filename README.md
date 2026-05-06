# daedongje-yonsei-server

연세대학교 개교 141주년 무악대동제 2026 백엔드 서버

> 이 README는 개발 기간 동안 Linear 사용 가이드로 운영됩니다. 런칭 후 프로젝트 소개 문서로 교체 예정.

---

## Linear 워크스페이스

- **워크스페이스:** [daedongje-yonsei-server](https://linear.app/daedongje-yonsei-server)
- **팀:** Back
- **GitHub 연동:** 양방향 싱크 (Linear ↔ GitHub Issues)

---

## 팀 구성

| 팀 | 담당 도메인 | 팀장 | Linear 라벨 |
| --- | --- | --- | --- |
| A | 부스·예약 (B, B-A, R, R-A) | 고선태 | `Team-A` |
| B | 공연·정보·지도 (P, P-A, I, I-A, 지도) | 이수정 | `Team-B` |
| C | 공통 플랫폼 (A, H, 공통) | 백세빈 | `Team-C` |
| Lead | 크로스 설계·리뷰·공통 모듈 | 우태호 | — |

---

## 이슈 생성 규칙

### 제목
```
[Function ID] 설명 (한글 OK)
```
예시:
- `[R-02] 예약 프로세스 API 구현`
- `[B-01] 부스 일자별/장소별 필터`
- `[P-A-02] 공연 타임테이블 등록 어드민`

### 라벨 (필수 2개)
1. **팀 라벨** — `Team-A`, `Team-B`, `Team-C` 중 하나
2. **기능 라벨** — `B-부스`, `R-예약`, `P-공연`, `I-정보`, `A-인증`, `H-홈`, `공통` 중 하나

### 타입
Linear 기본 라벨 사용: `Feature`, `Bug`, `Improvement`

### 본문 양식

Linear 이슈 본문은 아래 양식을 기본으로 사용한다. 해당 없는 섹션은 삭제해도 됨.

```markdown
## 배경 / 목적
<!-- 왜 이 작업이 필요한지 한두 줄로 -->

## 작업 내용
- [ ]

## 완료 조건 (DoD)
-

## 참고 자료
-
```

- **Bug** 타입은 위 양식에 `## 재현 경로`, `## 기대 동작 vs 실제 동작`, `## 환경` 섹션을 추가한다.
- **Improvement** 타입은 `## 현재 상태`, `## 개선 방향` 섹션을 추가한다.

---

## 브랜치 규칙

### 자동 생성
Linear 이슈 상세 → **Create branch** 클릭 시 자동 생성

포맷: `feature/{identifier}-{title}`
```
feature/BACK-12-booth-crud
feature/BACK-15-performance-timetable
```

### 주의사항
- **브랜치명은 반드시 영어로.** 자동 생성 시 한글이 들어가면 영어로 수정 후 생성
- 브랜치는 반드시 `dev`에서 분기

### 브랜치 전략
| 브랜치 | 용도 |
| --- | --- |
| `main` | 프로덕션 (리드 승인 후 머지) |
| `dev` | 통합 브랜치 (매주 금 스테이징 배포) |
| `feature/*` | 기능 개발 |
| `hotfix/*` | 프로덕션 긴급 수정 |

---

## PR 규칙

### PR 생성
- PR description에 **Linear 이슈 링크** 또는 `closes #이슈번호` 포함
- PR 제목에 이슈 키 포함 권장: `[BACK-12] 부스 CRUD API`

### 자동 연동
- PR 생성 → Linear 이슈 상태 자동 `In Review`
- PR 머지 → Linear 이슈 상태 자동 `Done` + GitHub 이슈 Close

### 리뷰
- 각 팀 팀장 리뷰 필수
- 다른 팀 도메인 파일 수정 시 → 해당 팀 팀장 필수 리뷰어

---

## 상태 흐름

```
Backlog → Todo → In Progress → In Review → Done
```

- **Backlog**: 아직 착수 안 한 이슈
- **Todo**: 이번 주기에 할 일
- **In Progress**: 작업 중 (브랜치 생성 시 자동 전이)
- **In Review**: PR 올린 상태 (PR 생성 시 자동 전이)
- **Done**: 머지 완료 (PR 머지 시 자동 전이)

---

## 커밋 메시지 컨벤션

```
feat: 부스 목록 조회 API 구현 (BACK-12)
fix: 예약 대기번호 중복 채번 버그 수정 (BACK-23)
chore: CI 워크플로우 설정 (BACK-8)
docs: API 스펙 문서 업데이트
refactor: BoothService 캐싱 로직 분리
test: 예약 동시성 테스트 추가
```

커밋에 이슈 키(`BACK-XX`)를 포함하면 Linear에서 해당 이슈에 커밋 히스토리가 자동 표시됩니다.

---

## 로컬 개발 시작하기

### 사전 요구사항

- **JDK 17** (Temurin 권장) — `java -version`으로 확인
- **Docker Desktop** — [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) (학생 무료)

### 1. 로컬 MySQL 띄우기

```bash
# 백그라운드로 컨테이너 기동
docker compose up -d

# 컨테이너 상태 확인 (Up + healthy 면 OK)
docker compose ps

# 종료 (데이터는 보존)
docker compose down

# 데이터까지 완전 초기화
docker compose down -v
```

기본 접속 정보 (`docker-compose.yml`에 정의):

| 항목 | 값 |
| --- | --- |
| Host | `localhost` |
| Port | `3306` |
| Database | `daedongje` |
| Username | `daedongje` |
| Password | `daedongje` |

### 2. 애플리케이션 실행

`application.yaml`의 datasource는 위 docker-compose 기본값을 그대로 사용한다 — **별도 환경변수 설정 없이 바로 실행 가능**.

```bash
./gradlew bootRun
```

운영(RDS) 등 다른 DB 사용 시 환경변수로 오버라이드:

```bash
DB_URL=jdbc:mysql://my-rds-endpoint:3306/daedongje \
DB_USERNAME=produser \
DB_PASSWORD=secret \
./gradlew bootRun
```

### 3. Swagger UI 확인

앱 기동 후 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) 접속.

---

## 데이터베이스 마이그레이션 (Flyway)

스키마 변경은 코드와 함께 버전 관리된다. JPA `ddl-auto`는 `validate`로만 동작 — 직접 테이블을 만들거나 변경하지 않는다.

### 마이그레이션 파일 위치

```
src/main/resources/db/migration/
├── V1__init.sql                     ← 베이스라인 (실질적으로 비어있음 — `SELECT 1` placeholder만 포함)
├── V2__create_booth_table.sql       ← 도메인 PR 에서 추가될 예시
├── V3__create_reservation_table.sql
└── ...
```

### 새 마이그레이션 추가 절차

1. 다음 버전 번호 확인 (현재 디렉토리에서 가장 큰 V 번호 + 1)
2. 파일 생성: `V{번호}__{스네이크_케이스_설명}.sql`
   - 예: `V2__create_booth_table.sql`
3. SQL 작성 (DDL/DML 모두 가능)
4. 앱 기동 — Flyway 가 자동으로 미적용 파일을 순서대로 실행
5. 적용 확인: `flyway_schema_history` 테이블 조회

### 절대 하지 말 것

- ❌ **이미 머지된 V 파일을 수정** — 적용된 환경에서 다시 실행되지 않아 환경 간 불일치 발생. 변경이 필요하면 새 V 파일 추가 (예: `V5__alter_booth_add_email.sql`).
- ❌ **버전 번호 건너뛰기** — Flyway 는 순차 적용. V2 다음에 V4 만들면 환경에 따라 동작 다름.
- ❌ **로컬에서 테이블 직접 만들기** — 다른 팀원과 스키마 불일치 발생.

### 작성 예시

```sql
-- V2__create_booth_table.sql
CREATE TABLE booth (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    location    VARCHAR(200),
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    INDEX idx_booth_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 테스트 실행

```bash
./gradlew test
```

- 테스트는 H2 인메모리 DB (`MODE=MySQL`)를 사용 — Docker MySQL 안 띄워도 즉시 실행 가능
- JPA `ddl-auto=create-drop` 으로 엔티티 정의에서 스키마 자동 생성
- Flyway 는 테스트에서 비활성화 (운영 마이그레이션 흐름 검증은 추후 Testcontainers 통합 테스트로)

