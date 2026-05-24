# 서버 관측 인프라 (Prometheus·Loki·Grafana Cloud·Discord) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 단일 EC2의 Spring 앱이 메트릭·로그를 Grafana Cloud로 송출하게 만들고(Phase 1), Grafana Cloud에서 임계치 알림 → Discord + 우리 웹훅으로 발화하도록 구성한다(Phase 2). Phase 3 관리자 API(이미 구현됨)가 이 위에서 동작한다.

**Architecture:** 앱에 `micrometer-registry-prometheus`를 추가해 `/actuator/prometheus`를 노출하고, 운영 프로파일에서 콘솔 로그를 ECS JSON으로 구조화한다. EC2에 가벼운 **Grafana Alloy 컨테이너**를 1개 추가해 도커 네트워크 내부에서 `app:8080/actuator/prometheus`를 scrape하고 컨테이너 stdout 로그를 tail → Grafana Cloud(Prometheus `remote_write` + Loki push)로 송출한다. Nginx는 `/actuator`를 외부 차단하되 `/internal`(Grafana 웹훅 수신)은 공개로 둔다. Grafana Cloud의 알림 룰·contact point·Discord 연결은 콘솔에서 설정하며 본 plan은 그 단계별 런북을 제공한다.

**Tech Stack:** Spring Boot 3.5.13, Java 17, Micrometer Prometheus, Spring Boot 3.5 내장 구조화 로깅(ECS), Grafana Alloy(도커), docker-compose, Nginx, GitHub Actions(배포), Grafana Cloud 무료 티어. 테스트: JUnit5 + AssertJ + `@SpringBootTest`.

**상위 스펙:** `docs/superpowers/specs/2026-05-24-server-observability-design.md` (Phase 1·2에 해당). Phase 3(앱 API)는 `docs/superpowers/plans/2026-05-24-admin-system-status-api.md`로 이미 구현 완료(PR #269).

**Linear/이슈:** BAC-135 / GitHub #270 / 브랜치 `feature/bac-135-common-server-observability-infra`.

---

## 선행조건 (코드 작업과 독립 — 병행 가능)

이 항목들은 **사람이 Grafana Cloud/Discord 콘솔에서** 확보해야 한다. 코드(Alloy 설정)는 전부 환경변수로 주입되므로 값이 없어도 빌드·테스트·머지는 가능하고, **운영 배포 시점에만** 필요하다. 상세 절차는 Task 8 런북 참조.

- [ ] Grafana Cloud 무료 계정 + Stack 생성.
- [ ] Prometheus `remote_write` URL + 인스턴스 ID(username).
- [ ] Loki push URL + 인스턴스 ID(username).
- [ ] Grafana Cloud Access Policy 토큰(`metrics:write`, `logs:write` 스코프) — Prom·Loki 공용 password로 사용.
- [ ] Discord 알림 채널 웹훅 URL.
- [ ] EC2 `.env`에 위 값 + `MONITORING_WEBHOOK_SECRET`(Phase 3에서 이미 정의) + `SPRING_PROFILES_ACTIVE=prod` 설정.

---

## File Structure

이번 작업은 **인프라·설정 중심**이라 신규 자바 클래스는 없고(테스트 제외), 빌드/설정/배포/문서 파일을 만지거나 추가한다.

```
build.gradle                                  # 수정 — micrometer-registry-prometheus 추가
src/main/resources/
├── application.yaml                          # 수정 — actuator prometheus 엔드포인트 노출
├── application-prod.yaml                     # 수정 — 구조화 JSON(ECS) 콘솔 로깅
└── logback-spring.xml                        # 수정 — 주석만 갱신(구조화 로깅은 property로 동작)

deploy/
├── alloy/config.alloy                        # 신규 — Alloy 수집기 설정(scrape + log tail → Grafana Cloud)
└── nginx/nginx.conf.example                  # 수정 — /actuator 외부 차단(/internal은 공개 유지)

docker-compose.prod.yml                       # 수정 — alloy 서비스 추가
.github/workflows/deploy.yml                  # 수정 — alloy 설정 파일 EC2 동기화
.env.example                                  # 수정 — GRAFANA_CLOUD_* placeholder 추가

docs/observability/grafana-cloud-setup.md     # 신규 — Phase 2 런북(알림 룰·Discord·웹훅 contact point)

src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/
├── PrometheusEndpointExposureTest.java       # 신규 — /actuator/prometheus 노출 검증
└── StructuredLoggingFormatTest.java          # 신규 — ECS JSON 콘솔 인코딩 검증
```

**책임 경계:**
- 메트릭 노출(Task 1)·구조화 로깅(Task 2)은 **앱 내부 설정**으로, 자동 테스트 가능.
- Alloy 설정(Task 3)·compose(Task 4)·배포 동기화(Task 5)·Nginx(Task 6)·env(Task 7)는 **인프라 파일**로, `validate`/`config` 명령과 운영 스모크로 검증.
- Grafana Cloud 알림·Discord(Task 8)는 **콘솔 설정**이라 런북 문서로 제공(코드 아님).

**의존 관계 메모:** Task 8의 "ERROR 로그 급증" LogQL 알림 룰(`| json | level="ERROR"`)은 Task 2의 구조화 JSON 로깅에 의존한다. Task 1의 `up` 메트릭은 Task 3 Alloy scrape job이 생성한다.

---

## Task 1: Prometheus 메트릭 엔드포인트 노출

**Files:**
- Modify: `build.gradle:60` (actuator 의존성 바로 아래)
- Modify: `src/main/resources/application.yaml:89-96` (management 블록)
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/PrometheusEndpointExposureTest.java`

- [ ] **Step 1: 실패하는 통합 테스트 작성**

`/actuator/prometheus`가 노출되어 표준 메트릭을 반환하는지 검증한다. `jvm_memory_used_bytes`는 앱 기동 시 항상 등록되므로 사전 요청 없이 단언 가능하다.

```java
package com.likelion.yonsei.daedongje.domain.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Alloy 수집기가 도커 네트워크 내부에서 scrape하는 /actuator/prometheus 가
 * 실제로 노출되고 Micrometer 표준 메트릭을 Prometheus 포맷으로 반환하는지 검증.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class PrometheusEndpointExposureTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("/actuator/prometheus 는 200 + Prometheus 포맷 메트릭을 반환한다")
    void prometheusEndpointIsExposed() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains("jvm_memory_used_bytes")
                .contains("# TYPE");
    }

    @Test
    @DisplayName("/actuator/health 는 계속 노출된다(배포 헬스체크 의존)")
    void healthEndpointStillExposed() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "*PrometheusEndpointExposureTest"`
Expected: FAIL — `/actuator/prometheus`가 404(미노출, 의존성·노출 설정 없음)라 `prometheusEndpointIsExposed`가 실패. (`healthEndpointStillExposed`는 통과할 수 있음.)

- [ ] **Step 3: build.gradle에 micrometer-registry-prometheus 추가**

`build.gradle`의 actuator 의존성(현재 60번 줄) 바로 아래에 추가. Spring Boot BOM이 버전을 관리하므로 버전 미지정.

```gradle
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
```

- [ ] **Step 4: application.yaml에서 prometheus 엔드포인트 노출**

`src/main/resources/application.yaml`의 management 블록(현재 89번 줄~)을 아래로 교체. `health`는 배포 헬스체크가 의존하므로 반드시 유지하고 `prometheus`를 추가한다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  endpoint:
    health:
      show-details: never
  prometheus:
    metrics:
      export:
        enabled: true
```

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "*PrometheusEndpointExposureTest"`
Expected: PASS — 두 테스트 모두 통과.

- [ ] **Step 6: 커밋 & 푸시**

```bash
git add build.gradle src/main/resources/application.yaml \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/PrometheusEndpointExposureTest.java
git commit -m "feat: Prometheus 메트릭 엔드포인트(/actuator/prometheus) 노출"
git push
```

---

## Task 2: 운영 프로파일 구조화 JSON 로깅(ECS)

**Files:**
- Modify: `src/main/resources/application-prod.yaml`
- Modify: `src/main/resources/logback-spring.xml:3` (주석 갱신)
- Test: `src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/StructuredLoggingFormatTest.java`

**왜 prod 한정:** 로컬/개발은 사람이 읽는 콘솔 로그가 편하므로 base/dev는 그대로 두고, JSON은 `application-prod.yaml`에만 켠다. Loki가 `| json` 파서로 라벨(`level` 등)을 뽑아 알림 룰에 쓰려면 운영 로그가 JSON이어야 한다(Task 8 LogQL 룰 의존).

**기존 logback-spring.xml과의 양립:** 현재 `logback-spring.xml`은 Spring Boot의 `console-appender.xml`을 `<include>`한다. 이 파일은 `CONSOLE_LOG_STRUCTURED_FORMAT` 시스템 프로퍼티(= `logging.structured.format.console`)를 읽어 인코더를 JSON으로 전환하므로, **property만 켜면** 커스텀 logback 설정을 건드리지 않고도 구조화 로깅이 적용된다. 이 테스트가 바로 그 양립성을 검증한다.

- [ ] **Step 1: 실패하는 테스트 작성**

`logging.structured.format.console=ecs` property가 켜지면 콘솔 출력이 ECS JSON이 되는지, `OutputCaptureExtension`으로 stdout을 캡처해 검증한다.

```java
package com.likelion.yonsei.daedongje.domain.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * 운영 프로파일이 켜는 logging.structured.format.console=ecs 가
 * 커스텀 logback-spring.xml(=Spring 기본 console-appender 포함)과 양립해
 * 콘솔에 ECS JSON 한 줄을 찍는지 검증.
 */
@SpringBootTest(webEnvironment = NONE, properties = "logging.structured.format.console=ecs")
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingFormatTest {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingFormatTest.class);

    @Test
    @DisplayName("구조화 로깅이 켜지면 콘솔 출력이 ECS JSON 형식이다")
    void consoleEmitsEcsJson(CapturedOutput output) {
        log.info("structured-logging-probe");

        assertThat(output.getOut())
                .contains("\"message\":\"structured-logging-probe\"")
                .contains("\"ecs.version\"")
                .contains("\"log.level\":\"INFO\"");
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "*StructuredLoggingFormatTest"`
Expected: FAIL — property를 켰는데도 출력이 평문이면(=인코더 미전환) 단언 실패. (의존성/Spring Boot 버전 문제로 ECS 포맷 자체가 안 되면 여기서 드러난다. Spring Boot 3.4+는 ECS 내장 지원, 본 레포는 3.5.13이라 충족.)

> 참고: 이 테스트는 코드 변경 전에도 `properties`로 직접 켜므로 **이미 통과할 수 있다**(양립성이 이미 성립). 그 경우 이 테스트는 "회귀 가드"로 남기고, 실제 변경은 Step 3(운영 프로파일에 영구 적용)이다. Step 2에서 통과하면 그대로 Step 3로 진행한다.

- [ ] **Step 3: application-prod.yaml에 구조화 로깅 추가**

`src/main/resources/application-prod.yaml` 끝에 추가.

```yaml

# 운영 콘솔 로그를 ECS JSON으로 구조화 → Alloy가 tail → Loki 라벨 파싱(| json).
# 로컬/개발(base·dev 프로파일)은 사람이 읽는 평문 콘솔 로그를 유지한다.
logging:
  structured:
    format:
      console: ecs
```

- [ ] **Step 4: logback-spring.xml 주석 갱신**

`src/main/resources/logback-spring.xml`의 3번 줄 주석이 "Phase 1 인프라에서 구조화 JSON으로 교체 예정"이라 현실과 어긋난다. 아래로 교체(코드 동작은 불변 — Spring 기본 console-appender가 property로 인코더를 전환).

```xml
    <!-- Spring Boot 기본 콘솔 로깅. 운영(prod) 프로파일에선 logging.structured.format.console=ecs
         가 이 console-appender 인코더를 ECS JSON으로 전환한다(application-prod.yaml). -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
```

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "*StructuredLoggingFormatTest"`
Expected: PASS.

- [ ] **Step 6: 운영 프로파일 수동 스모크(선택, 강력 권장)**

운영에서 실제로 JSON이 나오는지 로컬에서 prod 프로파일로 1회 확인.

Run: `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun` (DB/Redis 연결 실패는 무시 가능 — 기동 초기 로그만 확인)
Expected: 콘솔 첫 로그 줄들이 `{"@timestamp":...,"ecs.version":...}` 형태의 JSON.

- [ ] **Step 7: 커밋 & 푸시**

```bash
git add src/main/resources/application-prod.yaml src/main/resources/logback-spring.xml \
        src/test/java/com/likelion/yonsei/daedongje/domain/monitoring/StructuredLoggingFormatTest.java
git commit -m "feat: 운영 프로파일 콘솔 로그 ECS JSON 구조화"
git push
```

---

## Task 3: Grafana Alloy 수집기 설정 파일

**Files:**
- Create: `deploy/alloy/config.alloy`

**역할:** 도커 네트워크 내부에서 `app:8080/actuator/prometheus`를 scrape해 Grafana Cloud로 `remote_write`하고, 도커 컨테이너 stdout 로그를 tail해 Loki로 push한다. 모든 자격증명은 환경변수(`sys.env`)로 주입 — 파일에 비밀값을 넣지 않는다.

- [ ] **Step 1: config.alloy 작성**

```alloy
// ── 메트릭: app의 Actuator Prometheus 엔드포인트 scrape → Grafana Cloud remote_write ──
prometheus.scrape "app" {
  targets = [
    {
      __address__      = "app:8080",
      __metrics_path__ = "/actuator/prometheus",
      service          = "daedongje",
    },
  ]
  scrape_interval = "15s"
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
}

prometheus.remote_write "grafana_cloud" {
  endpoint {
    url = sys.env("GRAFANA_CLOUD_PROM_URL")
    basic_auth {
      username = sys.env("GRAFANA_CLOUD_PROM_USER")
      password = sys.env("GRAFANA_CLOUD_API_KEY")
    }
  }
}

// ── 로그: 도커 컨테이너 stdout tail → Loki ──
discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

// 컨테이너 이름(/unit-y-server 등)을 라벨로 정규화하고, app 컨테이너엔 service=daedongje 부여.
discovery.relabel "containers" {
  targets = discovery.docker.containers.targets

  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = "/(.*)"
    target_label  = "container"
  }
  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = "/unit-y-server"
    target_label  = "service"
    replacement   = "daedongje"
  }
}

loki.source.docker "containers" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.relabel.containers.output
  forward_to = [loki.write.grafana_cloud.receiver]
}

loki.write "grafana_cloud" {
  endpoint {
    url = sys.env("GRAFANA_CLOUD_LOKI_URL")
    basic_auth {
      username = sys.env("GRAFANA_CLOUD_LOKI_USER")
      password = sys.env("GRAFANA_CLOUD_API_KEY")
    }
  }
}
```

- [ ] **Step 2: Alloy 설정 문법 검증**

로컬에 docker가 있으면 Alloy 컨테이너로 문법만 검증한다(자격증명 불필요 — `validate`는 `sys.env`를 평가하지 않음).

Run:
```bash
docker run --rm -v "$(pwd)/deploy/alloy/config.alloy:/etc/alloy/config.alloy:ro" \
  grafana/alloy:latest validate /etc/alloy/config.alloy
```
Expected: `validation succeeded` (또는 비-에러 종료). 문법 오류가 나오면 메시지에 따라 컴포넌트/인자명을 수정 후 재실행. (Alloy DSL은 버전별 컴포넌트 인자가 미세하게 다를 수 있으니, 검증 결과를 진실로 삼아 이 파일을 맞춘다.)

- [ ] **Step 3: 커밋 & 푸시**

```bash
git add deploy/alloy/config.alloy
git commit -m "feat: Grafana Alloy 수집기 설정 추가 (메트릭 scrape + 로그 tail)"
git push
```

---

## Task 4: docker-compose.prod.yml에 Alloy 서비스 추가

**Files:**
- Modify: `docker-compose.prod.yml`

**메모(이미지 태그):** `grafana/alloy:latest`는 재현성이 낮다. 배포 전 현재 stable 태그로 고정할 것.

- [ ] **Step 1: alloy 서비스 추가**

`docker-compose.prod.yml` 끝(redis 서비스 아래)에 추가. app·redis와 같은 기본 compose 네트워크에 붙어 `app:8080`에 도달한다. 로그 tail을 위해 도커 소켓을 읽기전용 마운트하고, 자격증명은 기존 `.env`에서 주입한다.

```yaml
  alloy:
    image: grafana/alloy:latest  # TODO: 배포 전 현재 stable 태그로 고정 (https://hub.docker.com/r/grafana/alloy/tags)
    container_name: unit-y-alloy
    env_file:
      - .env
    volumes:
      - ./alloy/config.alloy:/etc/alloy/config.alloy:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - alloy-data:/var/lib/alloy/data
    command:
      - run
      - /etc/alloy/config.alloy
      - --storage.path=/var/lib/alloy/data
    depends_on:
      - app
    restart: unless-stopped

volumes:
  alloy-data:
```

> 주의: 기존 `docker-compose.prod.yml`에는 `volumes:` 최상위 키가 없다. 위 블록의 `volumes:`는 **파일 최상위**에 한 번만 와야 한다(services와 같은 들여쓰기 레벨). 서비스 블록 안의 `volumes:`(마운트 목록)와 혼동하지 말 것.

- [ ] **Step 2: compose 파일 유효성 검증**

Run:
```bash
DOCKER_IMAGE=placeholder/image:tag docker compose -f docker-compose.prod.yml config >/dev/null && echo "compose OK"
```
Expected: `compose OK` (YAML·스키마 유효, 변수 보간 성공). 에러 시 들여쓰기/키 위치 수정.

- [ ] **Step 3: 커밋 & 푸시**

```bash
git add docker-compose.prod.yml
git commit -m "feat: 운영 compose에 Alloy 수집기 컨테이너 추가"
git push
```

---

## Task 5: 배포 워크플로우 — Alloy 설정 EC2 동기화

**Files:**
- Modify: `.github/workflows/deploy.yml:66-72` ("Sync production compose file" 스텝)

**왜 필요:** 현재 워크플로우는 `docker-compose.prod.yml`만 EC2로 scp한다. `docker-compose.prod.yml`이 참조하는 `./alloy/config.alloy`가 EC2에 없으면 alloy 컨테이너 기동이 실패한다. 동기화 스텝에서 alloy 디렉터리를 만들고 설정 파일을 함께 보낸다.

- [ ] **Step 1: "Sync production compose file" 스텝 교체**

`.github/workflows/deploy.yml`의 해당 스텝을 아래로 교체.

```yaml
      - name: Sync production compose & Alloy config
        env:
          EC2_HOST: ${{ secrets.EC2_HOST }}
          EC2_USERNAME: ${{ secrets.EC2_USERNAME }}
        run: |
          ssh -i ~/.ssh/ec2_key "$EC2_USERNAME@$EC2_HOST" \
              'mkdir -p ~/unit-y-server/alloy'
          scp -i ~/.ssh/ec2_key docker-compose.prod.yml \
              "$EC2_USERNAME@$EC2_HOST:~/unit-y-server/docker-compose.prod.yml"
          scp -i ~/.ssh/ec2_key deploy/alloy/config.alloy \
              "$EC2_USERNAME@$EC2_HOST:~/unit-y-server/alloy/config.alloy"
```

- [ ] **Step 2: YAML 유효성 확인**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/deploy.yml')); print('yaml OK')"`
Expected: `yaml OK`.

- [ ] **Step 3: Deploy 잡이 alloy도 띄우는지 확인(런북 연계)**

`deploy.yml`의 "Deploy with Docker Compose" 스텝은 `up -d --no-deps --force-recreate app`로 **app만** 재생성한다. alloy는 최초 1회 또는 설정 변경 시 EC2에서 `docker compose -f docker-compose.prod.yml up -d alloy`로 띄워야 한다. 이는 Task 8 런북의 "최초 기동" 절차에 포함한다(워크플로우 자동화는 범위 밖 — alloy는 코드 배포마다 재생성할 필요 없음).

- [ ] **Step 4: 커밋 & 푸시**

```bash
git add .github/workflows/deploy.yml
git commit -m "ci: 배포 시 Alloy 설정 파일 EC2 동기화 추가"
git push
```

---

## Task 6: Nginx에서 /actuator 외부 차단

**Files:**
- Modify: `deploy/nginx/nginx.conf.example`

**핵심 구분:** `/actuator/**`는 메트릭·내부 정보라 외부에 절대 노출 금지(Alloy는 Nginx를 거치지 않고 도커 내부에서 직접 scrape). 반면 `/internal/monitoring/alerts`는 **Grafana Cloud가 인터넷에서 호출**하므로 공개 도달이 필요하다(공유 시크릿으로 보호 — Phase 3). 따라서 `/internal`은 차단하지 않는다.

> 배포 헬스체크(`deploy.yml`)는 `127.0.0.1:8080/actuator/health`를 **직접** 호출하므로 Nginx `/actuator` 차단의 영향을 받지 않는다.

- [ ] **Step 1: nginx.conf.example에 deny 블록 추가**

`deploy/nginx/nginx.conf.example`을 아래로 교체. `location /actuator`는 `location /`보다 구체적이라 우선 매칭된다.

```nginx
server {
    listen 80;
    server_name _;

    # 메트릭/내부 액추에이터는 외부 노출 금지. Alloy는 도커 내부에서 직접 scrape하므로 영향 없음.
    # (/internal/** 은 Grafana Cloud 웹훅 수신용이라 공개 유지 — 공유 시크릿으로 보호)
    location /actuator {
        deny all;
        return 404;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

- [ ] **Step 2: 적용 절차 메모(런북 연계)**

`nginx.conf.example`은 예시 파일이며 배포 워크플로우가 EC2로 동기화하지 않는다(EC2에서 수동 관리). EC2 적용은 Task 8 런북에 포함: 파일 갱신 후 `sudo nginx -t && sudo systemctl reload nginx`.

- [ ] **Step 3: 커밋 & 푸시**

```bash
git add deploy/nginx/nginx.conf.example
git commit -m "feat: Nginx에서 /actuator 외부 차단 (/internal 공개 유지)"
git push
```

---

## Task 7: .env.example에 Grafana Cloud 자격증명 placeholder 추가

**Files:**
- Modify: `.env.example`

- [ ] **Step 1: Grafana Cloud 변수 추가**

`.env.example` 끝(MONITORING_WEBHOOK_SECRET 아래)에 추가. Alloy `config.alloy`의 `sys.env(...)` 키와 정확히 일치해야 한다.

```bash

# Grafana Cloud (Alloy 수집기가 메트릭·로그를 송출할 때 사용)
# 값은 Grafana Cloud > Stack 상세 또는 Access Policy 토큰에서 확보. 절차: docs/observability/grafana-cloud-setup.md
GRAFANA_CLOUD_PROM_URL=https://prometheus-prod-XX-prod-REGION.grafana.net/api/prom/push
GRAFANA_CLOUD_PROM_USER=000000
GRAFANA_CLOUD_LOKI_URL=https://logs-prod-XXX.grafana.net/loki/api/v1/push
GRAFANA_CLOUD_LOKI_USER=000000
GRAFANA_CLOUD_API_KEY=glc_REPLACE_WITH_ACCESS_POLICY_TOKEN
```

- [ ] **Step 2: 키 일치 확인**

Run: `grep -oE 'GRAFANA_CLOUD_[A-Z_]+' deploy/alloy/config.alloy | sort -u; echo "---"; grep -oE 'GRAFANA_CLOUD_[A-Z_]+' .env.example | sort -u`
Expected: 두 목록이 동일(`GRAFANA_CLOUD_API_KEY`, `GRAFANA_CLOUD_LOKI_URL`, `GRAFANA_CLOUD_LOKI_USER`, `GRAFANA_CLOUD_PROM_URL`, `GRAFANA_CLOUD_PROM_USER`).

- [ ] **Step 3: 커밋 & 푸시**

```bash
git add .env.example
git commit -m "docs: .env.example에 Grafana Cloud 자격증명 placeholder 추가"
git push
```

---

## Task 8: Grafana Cloud 알림·Discord 설정 런북 (Phase 2)

**Files:**
- Create: `docs/observability/grafana-cloud-setup.md`

**성격:** Grafana Cloud 알림 룰·contact point·Discord 연결은 콘솔(또는 Terraform)에서 사람이 수행하는 운영 설정이라 코드가 아니다. 본 Task는 재현 가능한 **단계별 런북**을 문서로 남긴다.

- [ ] **Step 1: 런북 문서 작성**

아래 내용으로 `docs/observability/grafana-cloud-setup.md` 생성.

````markdown
# Grafana Cloud 관측 인프라 설정 런북

상위 설계: `docs/superpowers/specs/2026-05-24-server-observability-design.md`
구현 plan: `docs/superpowers/plans/2026-05-25-server-observability-infra.md`

이 문서는 **Grafana Cloud 콘솔/EC2에서 사람이 수행**하는 일회성 운영 설정을 정리한다. 코드(Alloy·앱)는 별도 PR로 머지됨.

## 1. Grafana Cloud 자격증명 확보

1. Grafana Cloud 무료 계정 생성 → Stack 1개 생성.
2. **Prometheus**: Stack > Prometheus > "Send Metrics"에서 `remote_write` URL과 username(인스턴스 ID) 확인 → `GRAFANA_CLOUD_PROM_URL`, `GRAFANA_CLOUD_PROM_USER`.
3. **Loki**: Stack > Loki > "Send Logs"에서 push URL과 username 확인 → `GRAFANA_CLOUD_LOKI_URL`, `GRAFANA_CLOUD_LOKI_USER`.
4. **토큰**: Access Policies에서 `metrics:write` + `logs:write` 스코프 토큰 발급 → `GRAFANA_CLOUD_API_KEY` (Prom·Loki 공용 password).

## 2. EC2 .env 설정

`~/unit-y-server/.env`에 아래를 채운다(값은 1번에서 확보):

```
SPRING_PROFILES_ACTIVE=prod
MONITORING_WEBHOOK_SECRET=<강한 랜덤 시크릿>
GRAFANA_CLOUD_PROM_URL=...
GRAFANA_CLOUD_PROM_USER=...
GRAFANA_CLOUD_LOKI_URL=...
GRAFANA_CLOUD_LOKI_USER=...
GRAFANA_CLOUD_API_KEY=...
```

## 3. Alloy 최초 기동 + 송출 확인

```bash
cd ~/unit-y-server
docker compose -f docker-compose.prod.yml up -d alloy
docker logs -f unit-y-alloy        # 인증/송출 에러 없는지 확인
```

확인: Grafana Cloud > Explore에서
- 메트릭: `up{service="daedongje"}` 가 1로 보이면 scrape·remote_write 성공.
- 로그: `{service="daedongje"} | json` 로 JSON 로그가 들어오면 Loki push 성공.

## 4. Nginx /actuator 차단 적용

```bash
# deploy/nginx/nginx.conf.example 기준으로 EC2 nginx 설정 갱신 후
sudo nginx -t && sudo systemctl reload nginx
curl -i http://<공인도메인>/actuator/health     # 404 여야 정상(외부 차단됨)
curl -i http://127.0.0.1:8080/actuator/health   # UP 이어야 정상(내부는 접근)
```

## 5. Contact point 2개 등록 (Alerting > Contact points)

1. **Discord**: 타입 Discord, Webhook URL = 디스코드 채널 웹훅 URL.
2. **우리 웹훅**: 타입 Webhook, URL = `https://<공인도메인>/internal/monitoring/alerts`,
   HTTP Method POST, 헤더 `Authorization: Bearer <MONITORING_WEBHOOK_SECRET>`.
   (시크릿은 2번 EC2 .env의 값과 동일해야 함 — 불일치 시 앱이 401로 거부.)

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
| ERROR 로그 급증 | Loki | `sum(count_over_time({service="daedongje"} \| json \| level=`"`ERROR`"` [5m]))` | `> N`(튜닝) | medium |

> "ERROR 로그 급증" 룰은 운영 로그가 ECS JSON일 때만 동작한다(구현 plan Task 2). `level` 필드명이 ECS에선 `log.level`일 수 있으니, Explore에서 실제 필드명을 확인해 `| json | log_level="ERROR"` 등으로 맞춘다.

## 8. 종단 검증

1. (앱 다운 룰) `docker stop unit-y-server` → 1분 후 Discord 알림 + `GET /api/admin/system/alerts`에 활성 알림 노출 → `docker start unit-y-server` → resolved 수신 시 목록에서 사라짐.
2. PII 점검: Explore에서 최근 로그를 훑어 전화번호 등 개인정보가 평문으로 들어오지 않는지 확인(스펙 §4.1). 새면 해당 로깅 지점 마스킹.
````

- [ ] **Step 2: 문서 링크 유효성 확인**

Run: `test -f docs/superpowers/specs/2026-05-24-server-observability-design.md && test -f docs/superpowers/plans/2026-05-25-server-observability-infra.md && echo "links OK"`
Expected: `links OK`.

- [ ] **Step 3: 커밋 & 푸시**

```bash
git add docs/observability/grafana-cloud-setup.md
git commit -m "docs: Grafana Cloud 알림·Discord 설정 런북 추가 (Phase 2)"
git push
```

---

## 최종 검증 (전체 빌드)

- [ ] **Step 1: 전체 테스트 통과 확인**

Run: `./gradlew clean test`
Expected: BUILD SUCCESSFUL — 신규 `PrometheusEndpointExposureTest`, `StructuredLoggingFormatTest` 포함 전부 통과.

- [ ] **Step 2: PR 생성**

CLAUDE.md PR 규칙 준수: `.github/PULL_REQUEST_TEMPLATE.md` 구조, 한국어, `closes #270`, Function ID는 이슈에 없으므로 사용자에게 재확인(공통 인프라 작업이라 N/A 가능). 자동 서명 푸터 금지.

---

## Self-Review (스펙 대비 커버리지)

스펙 `2026-05-24-server-observability-design.md` Phase 1·2 요구사항 대비:

- §4.1 메트릭: `micrometer-registry-prometheus` + `/actuator/prometheus` → **Task 1** ✅
- §4.1 로그: Spring Boot 내장 구조화 로깅(ECS) → **Task 2** ✅
- §4.1 Alloy scrape + 컨테이너 로그 tail → Grafana Cloud → **Task 3, 4** ✅
- §4.1 Nginx `/actuator` deny(+/internal 예외) → **Task 6** ✅
- §4.1 PII 점검 → **Task 8 §8** 검증 절차 ✅
- §4.2 알림 룰 7종 + Discord + 우리 웹훅 contact point → **Task 8** 런북 ✅
- 배포 파이프라인에 Alloy 설정 반영 → **Task 5** ✅ (스펙 미명시였으나 deploy.yml이 compose만 동기화해 누락 위험 → 보강)
- 선행조건(계정·웹훅·env) → 상단 체크리스트 + **Task 8** 런북 ✅

YAGNI(스펙 §10)대로 Tempo·ML 이상탐지·iframe 임베드·별도 모니터링 인스턴스는 범위 밖.
