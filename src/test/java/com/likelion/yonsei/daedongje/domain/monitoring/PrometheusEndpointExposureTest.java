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
