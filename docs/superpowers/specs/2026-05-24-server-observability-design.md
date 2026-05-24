# 서버 관측·이상탐지 시스템 설계

- **작성일:** 2026-05-24
- **상태:** 설계 확정 (구현 계획 수립 전)
- **대상 레포:** `daedongje-yonsei-server` (백엔드)
- **선행 조건:** Linear 이슈 생성 후 전용 브랜치에서 작업 (이 문서 커밋도 동일)

---

## 1. 목적과 배경

"무악대동제 2026" 백엔드 서버는 **축제 기간 며칠에 트래픽이 집중**되는 성격이다. 그 피크 구간에 발생하는 장애(에러율 급증, 응답 지연, DB 커넥션 풀 고갈, 메모리·CPU 포화)를 **놓치지 않고 즉시 인지**하고, 관리자가 "지금 서버가 괜찮은지"를 한눈에 확인할 수 있게 하는 것이 목표다.

현재 상태:
- Spring Boot 3.5.13 / Java 17, **단일 EC2 인스턴스**에 `docker-compose.prod.yml`로 `app` + `redis` 컨테이너, 앞단 Nginx(80 → 127.0.0.1:8080). DB는 외부 RDS(MySQL).
- Actuator는 의존성에 있으나 **`health` 엔드포인트만 노출**. Micrometer Prometheus 레지스트리 없음.
- 로그는 별도 설정 없이 **기본 콘솔 로그 → `docker logs`(stdout)** 로만 나감.

## 2. 핵심 설계 결정 (확정)

| # | 결정 | 선택 | 이유 |
|---|---|---|---|
| 1 | 이상 인지 방식 | **대시보드 + 임계치 알림(능동 푸시)** | 24h 사람이 못 붙는 축제에 즉시 인지 필요, 구축 부담 적정 |
| 2 | 모니터링 스택 위치 | **Grafana Cloud 무료 티어** | EC2 자원 경쟁 회피("장애 순간 감시 도구가 앱을 압박"), 인프라 손 최소 |
| 3 | 관리자 프론트 연동 | **커스텀 경량 "시스템 상태" 페이지 + Grafana 링크아웃** | 기존 관리자 세션 인증 재사용, Grafana 토큰 브라우저 비노출, 필요한 신호만 큐레이션 |
| 4 | 관리자 페이지 데이터 소스 | **인-프로세스(Actuator 직접) + 알림은 Grafana 웹훅 수신** | 장애 순간에도 외부 의존 없이 화면 생존, "이상의 정의"를 Grafana 룰 한 곳에서 단일 관리 |
| — | 알림 채널 | **Discord** | Grafana Cloud 기본 내장 contact point |

## 3. 전체 아키텍처

```
                                    ┌─────────────────────────────┐
                                    │       Grafana Cloud          │
                                    │  (무료 티어, 외부 위탁)       │
   EC2 (단일 인스턴스)               │  • Prometheus(메트릭 저장)    │
 ┌──────────────────────────┐       │  • Loki(로그 저장)           │
 │  app 컨테이너 (Spring)     │       │  • 대시보드                  │
 │   /actuator/prometheus ───┼──┐    │  • 알림 룰 평가              │
 │   stdout(JSON 구조화로그)  │  │    └──────┬───────────┬─────────┘
 │   /api/admin/system/* ◄───┼─┐│  scrape   │ 발화        │ 발화
 │   /internal/.../alerts ◄──┼┐││  +remote  ▼            ▼
 └──────────────────────────┘│││  write   [Discord]  [우리 웹훅]
 ┌──────────────────────────┐│││           (사람용)   (관리자 페이지용)
 │  alloy 컨테이너 (수집기)   ├┘││             │            │
 │   • app 메트릭 scrape      │ ││             │            └─POST─┐
 │   • 컨테이너 로그 tail      ├─┘│                                 │
 │   • Grafana Cloud로 송출    │  │                                 ▼
 └──────────────────────────┘  │                           [Spring이 Redis에
 ┌──────────────────────────┐  │                            활성 알림 TTL 저장]
 │  redis 컨테이너            │◄─┘ (세션 + 활성알림 저장)
 └──────────────────────────┘
        ▲
        │ GET /api/admin/system/health|errors|alerts (관리자 세션 가드)
 [관리자 프론트엔드] ── "시스템 상태" 페이지 + "Grafana 열기" 링크아웃
```

무거운 저장·대시보드·알림평가는 전부 Grafana Cloud가 맡고, EC2엔 **가벼운 Alloy 수집기 1개**만 추가된다(메모리 수십 MB). 관리자 화면용 라이브 스냅샷은 앱이 자기 자신에서 읽어 외부 의존 없이 동작한다.

## 4. 컴포넌트 설계

### 4.1 텔레메트리 파이프라인 (메트릭·로그 송출)

**메트릭**
- `micrometer-registry-prometheus` 의존성 추가 → `/actuator/prometheus` 노출.
- 표준 메트릭 자동 수집: `http.server.requests`(상태·uri·outcome 태그), `hikaricp.connections.*`, `jvm.memory.*`, `jvm.threads.*`, `system.cpu.usage`, `process.uptime`.
- Alloy가 **도커 네트워크 내부에서** `app:8080/actuator/prometheus`를 scrape → Grafana Cloud로 `remote_write`.

**로그**
- Spring Boot 3.5 **내장 구조화 로깅** 사용: `logging.structured.format.console=ecs` → 별도 라이브러리 없이 JSON 로그를 stdout으로.
- Alloy가 도커 컨테이너 로그를 tail → Loki로 송출(라벨: `service`, `level`, `container`).
- (선택) 요청별 상관관계용 MDC 요청ID 필터 → 로그 묶어보기.

**보안·운영 주의**
- ⚠️ Nginx에 `location /actuator { deny all; }` 추가. Alloy는 Nginx를 거치지 않고 컨테이너를 직접 scrape하므로 `/actuator/**`를 외부에 절대 노출하지 않는다.
- ⚠️ **PII**: 예약 도메인이 전화번호를 다룬다. JSON 로그에 전화번호·개인정보가 새지 않도록 로깅 지점을 점검(마스킹/제외).

### 4.2 이상탐지 = Grafana Cloud 알림 룰

Discord 푸시와 관리자 페이지가 공유하는 **단일 진실(single source of truth)**. 초기 룰(축제 부하테스트로 임계치 튜닝):

| 룰 | 조건(초기 예시) | 심각도 |
|---|---|---|
| 앱 다운 | `up == 0` 1분 지속 | critical |
| 5xx 에러율 급증 | `sum(rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) > 0.05` | high |
| 응답 지연 | `histogram_quantile(0.95, ...) > 2s` 5분 지속 | high |
| DB 풀 고갈 | `hikaricp_connections_pending > 0` 2분 지속 (예약 광클 시 1순위) | high |
| 힙 메모리 | 힙 사용률 > 90% 5분 지속 | medium |
| CPU | `system_cpu_usage > 0.9` 5분 지속 | medium |
| ERROR 로그 급증 | LogQL: `sum(count_over_time({service="daedongje"} \| json \| level="ERROR" [5m])) > N` | medium |

**Contact point 2개**: ① Discord(내장, 채널 웹훅 URL만 연결), ② 우리 Spring 웹훅(`/internal/monitoring/alerts`).

### 4.3 관리자 "시스템 상태" — 백엔드 API

> 관리자 프론트 화면 자체는 **FE 팀 작업**이다. 이 레포는 **API 계약 + 관측 인프라**를 제공한다.

기존 패키지 관례(`com.likelion.yonsei.daedongje.domain.*`)에 맞춰 `domain/monitoring` 신설.

**관리자용** — 기존 관리자 세션 가드 재사용. `BoothAdminController`와 동일하게 클래스 레벨 `@RequireAdminRole(...)` + 필요 시 `@CurrentAdmin AdminSessionUser`(`domain.auth.support`). 모니터링은 인프라 레벨이므로 **`AdminRole.SUPER`**(필요 시 `MASTER` 포함)로 제한 — 최종 역할 셋은 구현 시 확정.

| 메서드 | 경로 | 설명 | 데이터 출처 |
|---|---|---|---|
| GET | `/api/admin/system/health` | 라이브 스냅샷: status(UP/DOWN), uptime, 힙 used/max/%, HikariCP active/idle/pending/max, 스레드 수, CPU, 빌드 버전 | `MeterRegistry`·`HealthEndpoint`·`BuildProperties` 인-프로세스 |
| GET | `/api/admin/system/errors` | 최근 N개 ERROR 로그 | 메모리 링버퍼 |
| GET | `/api/admin/system/alerts` | 현재 활성 알림 목록 | Redis |

**내부 웹훅** — 관리자 세션 아님(Grafana Cloud가 인터넷에서 호출). **강한 시크릿/Bearer 헤더 검증 필수**, 불일치 시 401.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/internal/monitoring/alerts` | Grafana 웹훅 수신 → 활성 알림은 Redis에 TTL 저장, `resolved` 상태면 삭제 |

**컴포넌트(단일 책임)**
1. `SystemHealthService` — Micrometer 게이지 + Actuator Health를 스냅샷 DTO로 집계. 의존: `MeterRegistry`, `HealthEndpoint`, `BuildProperties`.
2. `RecentErrorLogBuffer` + **커스텀 Logback 어펜더** — 최근 N개 ERROR 이벤트 링버퍼. 스레드세이프 싱글톤; 어펜더가 write, 서비스가 read. `logback-spring.xml`에 어펜더 등록. (이 인스턴스 한정·재시작 시 휘발 — 영속 로그는 Loki가 담당.)
3. `ActiveAlertStore`(Redis) + `AlertWebhookController` — 알림 fingerprint 키로 upsert/resolve, TTL 부여.
4. `SystemStatusController`(관리자) — GET 3종.

## 5. 데이터 흐름

1. **정상 운영**: app이 메트릭/JSON로그 방출 → Alloy가 수집·송출 → Grafana Cloud 저장·대시보드. 관리자가 `/api/admin/system/health` 호출 → 앱이 인-프로세스 스냅샷 반환.
2. **이상 발생**: Grafana 알림 룰 발화 → ① Discord로 푸시(사람), ② 우리 웹훅으로 POST → `ActiveAlertStore`가 Redis에 TTL 저장 → 관리자 페이지 `/api/admin/system/alerts`에 노출.
3. **이상 해소**: Grafana가 `resolved` 웹훅 전송 → 해당 알림 Redis에서 삭제.
4. **Grafana Cloud 장애 시**: `/api/admin/system/health`·`/errors`는 인-프로세스라 계속 동작(스냅샷 정직). 신규 알림만 안 들어옴(=Discord도 동일).

## 6. 에러 처리

- 웹훅 시크릿 누락/불일치 → 401, 본문 무시.
- 웹훅 payload 파싱 실패 → 400 + 경고 로그, 저장 스킵(Grafana 재전송에 맡김).
- `MeterRegistry`에서 특정 메트릭 미존재 → 해당 필드 `null`로 두고 스냅샷은 정상 반환(부분 실패 허용).
- Redis 장애 → `/alerts`는 빈 목록 + 경고 로그(다운그레이드), `/health`는 영향 없음.

## 7. 테스트

- `SystemHealthService`: mock `MeterRegistry`/`HealthEndpoint` → 스냅샷 매핑·부분 실패(null 필드) 검증.
- `AlertWebhookController`: 정상 payload→Redis 반영, `resolved`→삭제, 시크릿 누락/오류→401, 깨진 payload→400.
- `RecentErrorLogBuffer`: 용량 초과 시 오래된 항목 축출, 동시 write 안전성.
- ⚠️ 테스트는 H2+ddl-auto·Flyway 비활성 환경(`project_test_schema_flyway_gap`). 이 기능은 대부분 비-DB(Redis/인메모리)라 영향 적음. `ActiveAlertStore`는 mock 또는 embedded Redis로 검증.

## 8. 단계(Phase)

이 설계는 인프라·앱 레이어가 섞여 있어 **2개의 Linear 이슈/구현 계획으로 분리** 가능(인프라 vs 앱 API). 구현 계획 수립 시 결정.

- **Phase 1 (인프라)**: `micrometer-registry-prometheus` 추가, `/actuator/prometheus` 노출 + Nginx deny, 구조화 로깅, Alloy 컨테이너 추가(`docker-compose.prod.yml`), Grafana Cloud 연결 → 메트릭·로그 흐르게.
- **Phase 2 (알림)**: Grafana 알림 룰 + Discord contact point + 우리 웹훅 contact point.
- **Phase 3 (앱 API)**: `domain/monitoring` — `SystemHealthService`, `RecentErrorLogBuffer`+어펜더, `ActiveAlertStore`+웹훅 수신기, `SystemStatusController`.
- **Phase 4 (FE, 타 레포)**: 관리자 "시스템 상태" 페이지 — 우리 API 소비. 본 레포 범위 밖, API 계약만 제공.

## 9. 사전 준비 / 미해결 항목

- [ ] Grafana Cloud 무료 계정 생성/확인.
- [ ] Discord 채널 웹훅 URL 확보.
- [ ] EC2 메모리 사양 확인(Alloy 여유 — 거의 문제없음).
- [ ] 알림 임계치: 위 기본값으로 시작 후 부하테스트로 튜닝.
- [ ] 웹훅 시크릿 관리 방식(`.env` 환경변수) 확정.
- [ ] 관리자 system 엔드포인트 허용 역할 셋(SUPER / +MASTER) 최종 확정.

## 10. 범위 밖 (YAGNI)

- 분산 트레이싱(Tempo) — 단일 인스턴스 모놀리스라 효용 낮음.
- 자동 이상 탐지(ML/베이스라인) — 며칠짜리 행사엔 학습 데이터·운영 부담 대비 효용 낮음.
- Grafana 대시보드 iframe 임베드 — Cloud에선 private 임베드가 까다롭고 public은 데이터 노출. 링크아웃으로 대체.
- 별도 모니터링 인스턴스 — Grafana Cloud 위탁으로 불필요.
