package com.likelion.yonsei.daedongje.domain.monitoring.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * logback-spring.xml 이 RECENT_ERROR 어펜더를 실제로 루트에 연결했는지 검증.
 * Spring Boot 컨텍스트가 로딩되어야 logback-spring.xml 이 적용된다.
 */
@SpringBootTest
class RecentErrorLogWiringTest {

    private static final Logger log = LoggerFactory.getLogger(RecentErrorLogWiringTest.class);

    @Test
    @DisplayName("ERROR 로그를 남기면 링버퍼에 적재된다 (어펜더가 루트에 연결됨)")
    void errorLogIsCapturedByAppender() {
        RecentErrorLogBuffer.getInstance().clear();

        log.error("wiring-test-boom");

        List<ErrorLogEntry> snapshot = RecentErrorLogBuffer.getInstance().snapshot();
        assertThat(snapshot)
                .extracting(ErrorLogEntry::message)
                .contains("wiring-test-boom");
    }

    @Test
    @DisplayName("INFO 로그는 ThresholdFilter에 막혀 링버퍼에 적재되지 않는다")
    void infoLogIsFilteredOut() {
        RecentErrorLogBuffer.getInstance().clear();

        log.info("wiring-test-info");

        assertThat(RecentErrorLogBuffer.getInstance().snapshot())
                .extracting(ErrorLogEntry::message)
                .doesNotContain("wiring-test-info");
    }
}
