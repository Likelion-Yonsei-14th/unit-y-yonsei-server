package com.likelion.yonsei.daedongje.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GlobalExceptionHandler} 슬라이스 테스트.
 *
 * <p>Spring 컨텍스트를 로드하지 않고 {@link MockMvcBuilders#standaloneSetup} 으로 핸들러와
 * 테스트 컨트롤러만 직접 와이어링한다. {@code @WebMvcTest} 슬라이스는 내부 정적 컨트롤러를
 * 안정적으로 등록하지 못해 정적 리소스 핸들러로 라우팅되는 문제가 있어서 이 방식을 채택.
 *
 * <p>테스트 항목:
 * <ul>
 *   <li>{@link BusinessException} → ErrorCode 의 status/code 매핑</li>
 *   <li>Bean Validation 실패 → COMMON-001 (400)</li>
 *   <li>지원되지 않는 HTTP 메서드 → COMMON-005 (405)</li>
 *   <li>예상치 못한 예외 → COMMON-500 (500)</li>
 * </ul>
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessException_은_ErrorCode_의_status_와_code_로_응답된다() throws Exception {
        mockMvc.perform(get("/_test/business-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-004"))
                .andExpect(jsonPath("$.error.message").value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    void bean_validation_실패는_INVALID_INPUT_으로_변환된다() throws Exception {
        mockMvc.perform(post("/_test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void 허용되지_않는_HTTP_메서드는_METHOD_NOT_ALLOWED_로_변환된다() throws Exception {
        mockMvc.perform(post("/_test/business-error"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-005"));
    }

    @Test
    void 예상치_못한_예외는_INTERNAL_ERROR_로_변환된다() throws Exception {
        mockMvc.perform(get("/_test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-500"));
    }

    @RestController
    @RequestMapping("/_test")
    static class TestController {

        @GetMapping("/business-error")
        public void throwBusiness() {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        @PostMapping("/validate")
        public void validate(@Valid @RequestBody Payload payload) {
            // body 가 유효성 통과해야만 도달 — 검증 실패는 핸들러가 가로챔
        }

        @GetMapping("/unexpected")
        public void throwUnexpected() {
            throw new IllegalStateException("intentional");
        }

        record Payload(@NotBlank String name) {
        }
    }
}
