# Grafana Cloud 관측 인프라 설정 런북

상위 설계: `docs/superpowers/specs/2026-05-24-server-observability-design.md`
구현 plan: `docs/superpowers/plans/2026-05-25-server-observability-infra.md`

이 문서는 **Grafana Cloud 콘솔/EC2에서 사람이 수행**하는 일회성 운영 설정을 정리한다. 코드(Alloy·앱)는 별도 PR로 머지됨(BAC-135).

## 1. Grafana Cloud 자격증명 확보

1. Grafana Cloud 무료 계정 생성 → Stack 1개 생성.
2. **Prometheus**: Stack > Prometheus > "Send Metrics"에서 `remote_write` URL과 username(인스턴스 ID) 확인 → `GRAFANA_CLOUD_PROM_URL`, `GRAFANA_CLOUD_PROM_USER`.
3. **Loki**: Stack > Loki > "Send Logs"에서 push URL과 username 확인 → `GRAFANA_CLOUD_LOKI_URL`, `GRAFANA_CLOUD_LOKI_USER`.
4. **토큰**: Access Policies에서 `metrics:write` + `logs:write` 스코프 토큰 발급 → `GRAFANA_CLOUD_API_KEY` (Prom·Loki 공용 password).

## 2. EC2 .env 설정

`~/unit-y-server/.env`에 아래를 채운다(값은 1번에서 확보). 키 목록은 레포 `.env.example` 참고.

```
SPRING_PROFILES_ACTIVE=prod
MONITORING_WEBHOOK_SECRET=<강한 랜덤 시크릿>
GRAFANA_CLOUD_PROM_URL=...
GRAFANA_CLOUD_PROM_USER=...
GRAFANA_CLOUD_LOKI_URL=...
GRAFANA_CLOUD_LOKI_USER=...
GRAFANA_CLOUD_API_KEY=...
```

> ⚠️ `SPRING_PROFILES_ACTIVE=prod` 가 설정돼야 구조화 JSON 로깅(ECS)이 켜진다(`logback-spring.xml` 의 `springProfile=prod`). dev/미설정이면 평문 로그가 나가고 Loki의 `| json` 파싱이 동작하지 않는다.

## 3. Alloy 최초 기동 + 송출 확인

```bash
cd ~/unit-y-server
docker compose -f docker-compose.prod.yml up -d alloy
docker logs -f unit-y-alloy        # 인증/송출 에러 없는지 확인
```

확인: Grafana Cloud > Explore에서
- 메트릭: `up{service="daedongje"}` 가 1로 보이면 scrape·remote_write 성공.
- 로그: `{service="daedongje"} | json` 로 JSON 로그가 들어오면 Loki push 성공.

> 배포 워크플로우(`deploy.yml`)는 코드 배포마다 `app` 컨테이너만 재생성한다. alloy는 최초 1회 또는 설정 변경 시에만 위 명령으로 띄우면 된다.

## 4. Nginx /actuator 차단 적용

레포 `deploy/nginx/nginx.conf.example` 기준으로 EC2 nginx 설정을 갱신한 뒤:

```bash
sudo nginx -t && sudo systemctl reload nginx
curl -i http://<공인도메인>/actuator/health     # 404 여야 정상(외부 차단됨)
curl -i http://127.0.0.1:8080/actuator/health   # UP 이어야 정상(내부는 접근)
```

> `/internal/monitoring/alerts`(Grafana 웹훅 수신)는 차단하지 않는다 — 공개 도달이 필요하며 공유 시크릿으로 보호된다.

## 5. Contact point 2개 등록 (Alerting > Contact points)

1. **Discord**: 타입 Discord, Webhook URL = 디스코드 채널 웹훅 URL.
2. **우리 웹훅**: 타입 Webhook, URL = `https://<공인도메인>/internal/monitoring/alerts`,
   HTTP Method POST, 헤더 `Authorization: Bearer <MONITORING_WEBHOOK_SECRET>`.
   (시크릿은 2번 EC2 `.env`의 값과 동일해야 함 — 불일치 시 앱이 401로 거부.)

## 6. Notification policy

기본 정책의 default contact point를 위 2개로 라우팅(또는 라벨 매칭으로 분기).
critical/high는 즉시, medium은 묶음(group) 전송 권장.

## 7. 알림 룰 등록 (Alerting > Alert rules)

스펙 §4.2 기준 초기 임계치(축제 부하테스트로 튜닝 예정). 데이터소스는 Stack의 Prometheus/Loki.

| 룰 | 데이터소스 | 식 | 조건 | 심각도 |
|---|---|---|---|---|
| 앱 다운 | Prometheus | `up{service="daedongje"}` | `== 0` for 1m | critical |
| 5xx 에러율 | Prometheus | `sum(rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))` | `> 0.05` | high |
| 응답 지연(p95) | Prometheus | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` | `> 2` for 5m | high |
| DB 풀 고갈 | Prometheus | `hikaricp_connections_pending` | `> 0` for 2m | high |
| 힙 메모리 | Prometheus | `sum(jvm_memory_used_bytes{area="heap"}) / sum(jvm_memory_max_bytes{area="heap"})` | `> 0.9` for 5m | medium |
| CPU | Prometheus | `system_cpu_usage` | `> 0.9` for 5m | medium |
| ERROR 로그 급증 | Loki | `sum(count_over_time({service="daedongje"} \| json \| log_level="ERROR" [5m]))` | `> N`(튜닝) | medium |

> **로그 라벨 주의**: 운영 로그는 ECS JSON이라 `level`이 중첩(`{"log":{"level":"ERROR"}}`)으로 나간다. Loki `| json` 파서는 중첩 키를 `_`로 평탄화하므로 필드명이 **`log_level`** 이 된다(위 LogQL 반영됨). Explore에서 실제 라벨명을 한 번 확인할 것.

## 8. 종단 검증

1. (앱 다운 룰) `docker stop unit-y-server` → 1분 후 Discord 알림 + `GET /api/admin/system/alerts`에 활성 알림 노출 → `docker start unit-y-server` → resolved 수신 시 목록에서 사라짐.
2. PII 점검: Explore에서 최근 로그를 훑어 전화번호 등 개인정보가 평문으로 들어오지 않는지 확인(스펙 §4.1). 새면 해당 로깅 지점 마스킹.
