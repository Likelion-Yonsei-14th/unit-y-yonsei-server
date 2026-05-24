package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.dto.SystemHealthResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/**
 * 앱 자신의 Micrometer 메트릭·Actuator 헬스·빌드 정보를 인-프로세스로 읽어
 * 관리자 시스템 상태 스냅샷을 만든다. 외부(Grafana Cloud) 의존 없음.
 */
@Service
public class SystemHealthService {

    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public SystemHealthService(MeterRegistry meterRegistry,
                               HealthEndpoint healthEndpoint,
                               ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.meterRegistry = meterRegistry;
        this.healthEndpoint = healthEndpoint;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    public SystemHealthResponse snapshot() {
        Long heapUsed = gaugeLong("jvm.memory.used", "area", "heap");
        Long heapMax = gaugeLong("jvm.memory.max", "area", "heap");
        Double heapRatio = (heapUsed != null && heapMax != null && heapMax > 0)
                ? (double) heapUsed / heapMax
                : null;

        SystemHealthResponse.MemoryInfo heap =
                new SystemHealthResponse.MemoryInfo(heapUsed, heapMax, heapRatio);

        SystemHealthResponse.DbPoolInfo dbPool = new SystemHealthResponse.DbPoolInfo(
                gaugeInt("hikaricp.connections.active"),
                gaugeInt("hikaricp.connections.idle"),
                gaugeInt("hikaricp.connections.pending"),
                gaugeInt("hikaricp.connections.max")
        );

        return new SystemHealthResponse(
                healthStatus(),
                version(),
                gaugeLong("process.uptime"),
                heap,
                dbPool,
                gaugeInt("jvm.threads.live"),
                gaugeDouble("system.cpu.usage")
        );
    }

    private String healthStatus() {
        try {
            HealthComponent health = healthEndpoint.health();
            Status status = (health == null) ? null : health.getStatus();
            return (status == null) ? null : status.getCode();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String version() {
        BuildProperties props = buildPropertiesProvider.getIfAvailable();
        return (props == null) ? null : props.getVersion();
    }

    /** 이름(+선택 태그 1쌍)에 매칭되는 게이지 값들의 합. 없으면 null. */
    private Double gaugeDouble(String name, String... tags) {
        Search search = meterRegistry.find(name);
        if (tags.length >= 2) {
            search = search.tag(tags[0], tags[1]);
        }
        double sum = 0;
        boolean found = false;
        for (Gauge g : search.gauges()) {
            double v = g.value();
            if (!Double.isNaN(v)) {
                sum += v;
                found = true;
            }
        }
        return found ? sum : null;
    }

    private Long gaugeLong(String name, String... tags) {
        Double v = gaugeDouble(name, tags);
        return (v == null) ? null : (long) (double) v;
    }

    private Integer gaugeInt(String name, String... tags) {
        Double v = gaugeDouble(name, tags);
        return (v == null) ? null : (int) (double) v;
    }
}
