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

