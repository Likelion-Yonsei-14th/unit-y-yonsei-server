package com.likelion.yonsei.daedongje.domain.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 로그가 ECS JSON으로 나가도록 하는 두 설정의 배선을 검증한다(결정적, Spring 컨텍스트 불필요).
 *
 * 실제 JSON 출력 동작은 prod 프로파일 단독 실행/수동 스모크로 입증됨. 여기서 런타임 어펜더를
 * 검사하지 않는 이유: Spring Boot 는 JVM당 logback 을 한 번만 초기화하므로, 전체 스위트에서
 * 먼저 뜬 비-prod 컨텍스트가 평문 어펜더를 고정시켜 어펜더 검사가 플레이키해진다.
 * 대신 "설정이 올바르게 배선됐는지" 계약을 고정해 회귀를 막는다.
 */
class StructuredLoggingFormatTest {

    @Test
    @DisplayName("logback-spring.xml 이 prod 프로파일에서 structured-console-appender 를 include 한다")
    void logbackWiresStructuredAppenderUnderProd() throws Exception {
        String logback;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("logback-spring.xml")) {
            assertThat(in).as("logback-spring.xml 이 클래스패스에 있어야 한다").isNotNull();
            logback = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(logback)
                .contains("<springProfile name=\"prod\">")
                .contains("structured-console-appender.xml");
    }

    @Test
    @DisplayName("application-prod.yaml 이 콘솔 구조화 포맷을 ecs 로 설정한다")
    void prodYamlSetsEcsConsoleFormat() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-prod.yaml"));
        Properties props = factory.getObject();

        assertThat(props).isNotNull();
        assertThat(props.getProperty("logging.structured.format.console")).isEqualTo("ecs");
    }
}
