package com.likelion.yonsei.daedongje.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
 * <p>각 예외 케이스에서 응답이 {@code ApiResponse} 형태로 일관되게 반환되는지,
 * HTTP status 와 error.code 매핑이 의도대로 동작하는지 검증한다.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

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
