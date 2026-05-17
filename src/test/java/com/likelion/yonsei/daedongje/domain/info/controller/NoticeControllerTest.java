package com.likelion.yonsei.daedongje.domain.info.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoticeRepository noticeRepository;

    @MockBean
    private AdminAuthContextService adminAuthContextService;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
        Mockito.reset(adminAuthContextService);
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(1L, AdminRole.MASTER, "notice-admin"));
    }

    @Test
    void 공지사항을_등록하고_목록에서_조회할_수_있다() throws Exception {
        String requestBody = """
                {
                  "title": "부스 운영 안내",
                  "content": "공지사항 본문입니다.",
                  "hasImage": true,
                  "isPinned": true,
                  "category": "GENERAL"
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("부스 운영 안내"))
                .andExpect(jsonPath("$.data.hasImage").value(true))
                .andExpect(jsonPath("$.data.imageUrl").value("pending-upload"))
                .andExpect(jsonPath("$.data.isPinned").value(true));

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("부스 운영 안내"));
    }

    @Test
    void 공지사항을_수정하고_삭제할_수_있다() throws Exception {
        String createBody = """
                {
                  "title": "수정 전",
                  "content": "초기 본문",
                  "hasImage": false,
                  "isPinned": false
                }
                """;

        String response = mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long noticeId = objectMapper.readTree(response).path("data").path("id").asLong();

        String updateBody = """
                {
                  "title": "수정 후",
                  "content": "수정된 본문",
                  "hasImage": true,
                  "imageUrl": "https://example.com/notice.png",
                  "isPinned": true,
                  "category": "EVENT"
                }
                """;

        mockMvc.perform(put("/api/admin/notices/{noticeId}", noticeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정 후"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/notice.png"))
                .andExpect(jsonPath("$.data.isPinned").value(true))
                .andExpect(jsonPath("$.data.category").value("EVENT"));

        mockMvc.perform(delete("/api/admin/notices/{noticeId}", noticeId))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void 관리자_세션이_없으면_공지사항_쓰기_API는_거부된다() throws Exception {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        String requestBody = """
                {
                  "title": "권한 테스트",
                  "content": "본문",
                  "hasImage": false,
                  "isPinned": false
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}
