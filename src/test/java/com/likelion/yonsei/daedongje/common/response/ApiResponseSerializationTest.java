package com.likelion.yonsei.daedongje.common.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiResponse 의 JSON 직렬화 contract 를 잠근다.
 *
 * <p>실 운영 직렬화 설정(스프링이 빌드한 {@code ObjectMapper}: 모듈 등록·전역 inclusion 등 포함)으로
 * 검증하기 위해 {@code @SpringBootTest(webEnvironment = NONE)} 로 컨테이너 주입 ObjectMapper 를 사용한다.
 * {@code @JsonTest} 슬라이스는 {@code @EnableJpaAuditing} 때문에 빈 JPA metamodel 로 실패하므로 회피.
 * 단언은 문자열 매칭 대신 {@code JsonNode} 트리로 — 키 존재 여부와 값 타입을 명시적으로 확인.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApiResponseSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successWithData_serializesDataAndExplicitNullError() throws Exception {
        ApiResponse<String> response = ApiResponse.success("hello");

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(node.get("success").booleanValue()).isTrue();
        assertThat(node.get("data").textValue()).isEqualTo("hello");
        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error").isNull()).isTrue();
    }

    @Test
    void successWithNullData_explicitlyIncludesNullDataAndNullError() throws Exception {
        ApiResponse<String> response = ApiResponse.success(null);

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(node.get("success").booleanValue()).isTrue();
        assertThat(node.has("data")).isTrue();
        assertThat(node.get("data").isNull()).isTrue();
        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error").isNull()).isTrue();
    }

    @Test
    void successEmpty_explicitlyIncludesNullDataAndNullError() throws Exception {
        ApiResponse<Void> response = ApiResponse.successEmpty();

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(node.get("success").booleanValue()).isTrue();
        assertThat(node.has("data")).isTrue();
        assertThat(node.get("data").isNull()).isTrue();
        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error").isNull()).isTrue();
    }

    @Test
    void error_explicitlyIncludesNullDataAndErrorObject() throws Exception {
        ApiResponse<Void> response = ApiResponse.error("B-001", "테스트 에러");

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(node.get("success").booleanValue()).isFalse();
        assertThat(node.has("data")).isTrue();
        assertThat(node.get("data").isNull()).isTrue();
        JsonNode errorNode = node.get("error");
        assertThat(errorNode.isNull()).isFalse();
        assertThat(errorNode.get("code").textValue()).isEqualTo("B-001");
        assertThat(errorNode.get("message").textValue()).isEqualTo("테스트 에러");
    }
}
