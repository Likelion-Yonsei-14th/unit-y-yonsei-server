package com.likelion.yonsei.daedongje.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successWithData_serializesDataAndNullError() throws Exception {
        ApiResponse<String> response = ApiResponse.success("hello");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"data\":\"hello\"");
        assertThat(json).contains("\"error\":null");
    }

    @Test
    void successWithNullData_explicitlyIncludesNullDataAndNullError() throws Exception {
        ApiResponse<String> response = ApiResponse.success(null);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"data\":null");
        assertThat(json).contains("\"error\":null");
    }

    @Test
    void successEmpty_includesNullDataAndNullError() throws Exception {
        ApiResponse<Void> response = ApiResponse.successEmpty();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"data\":null");
        assertThat(json).contains("\"error\":null");
    }

    @Test
    void error_includesNullDataAndErrorObject() throws Exception {
        ApiResponse<Void> response = ApiResponse.error("B-001", "테스트 에러");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"data\":null");
        assertThat(json).contains("\"error\":{");
        assertThat(json).contains("\"code\":\"B-001\"");
    }
}
