package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemHealthServiceTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<BuildProperties> buildPropsProvider(String version) {
        Properties p = new Properties();
        if (version != null) {
            p.setProperty("version", version);
        }
        BuildProperties props = version == null ? null : new BuildProperties(p);
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(props);
        return provider;
    }

    @Test
    @DisplayName("등록된 메트릭과 헬스/빌드 정보를 스냅샷으로 매핑한다")
    void mapsRegisteredMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Gauge.builder("jvm.memory.used", () -> 500_000_000.0).tag("area", "heap").register(registry);
        Gauge.builder("jvm.memory.max", () -> 1_000_000_000.0).tag("area", "heap").register(registry);
        Gauge.builder("hikaricp.connections.active", () -> 3.0).register(registry);
        Gauge.builder("hikaricp.connections.idle", () -> 5.0).register(registry);
        Gauge.builder("hikaricp.connections.pending", () -> 0.0).register(registry);
        Gauge.builder("hikaricp.connections.max", () -> 10.0).register(registry);
        Gauge.builder("jvm.threads.live", () -> 42.0).register(registry);
        Gauge.builder("system.cpu.usage", () -> 0.3).register(registry);
        Gauge.builder("process.uptime", () -> 3600.0).register(registry);

        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        SystemHealthService service =
                new SystemHealthService(registry, healthEndpoint, buildPropsProvider("1.2.3"));

        SystemHealthResponse r = service.snapshot();

        assertThat(r.status()).isEqualTo("UP");
        assertThat(r.version()).isEqualTo("1.2.3");
        assertThat(r.uptimeSeconds()).isEqualTo(3600L);
        assertThat(r.heap().usedBytes()).isEqualTo(500_000_000L);
        assertThat(r.heap().maxBytes()).isEqualTo(1_000_000_000L);
        assertThat(r.heap().usedRatio()).isEqualTo(0.5);
        assertThat(r.dbPool().active()).isEqualTo(3);
        assertThat(r.dbPool().max()).isEqualTo(10);
        assertThat(r.liveThreads()).isEqualTo(42);
        assertThat(r.cpuUsage()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("메트릭/빌드정보가 없으면 해당 필드는 null이고 상태는 그대로 반영된다")
    void missingMetricsBecomeNull() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.down().build());

        SystemHealthService service =
                new SystemHealthService(registry, healthEndpoint, buildPropsProvider(null));

        SystemHealthResponse r = service.snapshot();

        assertThat(r.status()).isEqualTo("DOWN");
        assertThat(r.version()).isNull();
        assertThat(r.heap().usedBytes()).isNull();
        assertThat(r.heap().usedRatio()).isNull();
        assertThat(r.dbPool().active()).isNull();
        assertThat(r.liveThreads()).isNull();
        assertThat(r.cpuUsage()).isNull();
    }

    @Test
    @DisplayName("헬스 조회가 예외를 던지면 status는 null로 폴백된다")
    void healthExceptionFallsBackToNullStatus() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenThrow(new RuntimeException("actuator down"));

        SystemHealthService service =
                new SystemHealthService(registry, healthEndpoint, buildPropsProvider("1.0.0"));

        SystemHealthResponse r = service.snapshot();

        assertThat(r.status()).isNull();
    }
}
