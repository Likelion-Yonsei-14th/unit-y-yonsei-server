package com.likelion.yonsei.daedongje.domain.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * prod 프로파일에서 logback-spring.xml 이 structured-console-appender 를 선택하고
 * application-prod.yaml 의 logging.structured.format.console=ecs 가 ECS 포맷을 제공해
 * 콘솔에 ECS JSON 한 줄을 찍는지 검증(=운영 로그가 실제로 JSON으로 나가는지).
 */
@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("prod")
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingFormatTest {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingFormatTest.class);

    @Test
    @DisplayName("구조화 로깅이 켜지면 콘솔 출력이 ECS JSON 형식이다")
    void consoleEmitsEcsJson(CapturedOutput output) {
        log.info("structured-logging-probe");

        // ECS 포맷은 중첩 객체: {"@timestamp":...,"log":{"level":"INFO",...},...,"ecs":{"version":"8.11"}}
        assertThat(output.getOut())
                .contains("\"@timestamp\"")
                .contains("\"message\":\"structured-logging-probe\"")
                .contains("\"log\":{\"level\":\"INFO\"")
                .contains("\"ecs\":{\"version\":");
    }
}
