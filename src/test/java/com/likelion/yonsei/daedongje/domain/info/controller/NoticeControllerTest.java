package com.likelion.yonsei.daedongje.domain.info.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;

import static org.hamcrest.Matchers.hasSize;
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

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
    }

    @Test
    void 공지사항을_등록하고_목록에서_조회할_수_있다() throws Exception {
        String requestBody = """
                {
                  "title": "부스 운영 안내",
                  "content": "공지사항 본문입니다.",
                  "has_image": true,
                  "is_pinned": true,
                  "category": "GENERAL"
                }
                """;

        mockMvc.perform(post("/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("부스 운영 안내"))
                .andExpect(jsonPath("$.data.has_image").value(true))
                .andExpect(jsonPath("$.data.image_url").value("pending-upload"))
                .andExpect(jsonPath("$.data.is_pinned").value(true));

        mockMvc.perform(get("/notices"))
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
                  "content": "초기 본문"
                }
                """;

        String response = mockMvc.perform(post("/notices")
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
                  "image_url": "https://example.com/notice.png",
                  "category": "EVENT"
                }
                """;

        mockMvc.perform(put("/notices/{noticeId}", noticeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정 후"))
                .andExpect(jsonPath("$.data.image_url").value("https://example.com/notice.png"))
                .andExpect(jsonPath("$.data.category").value("EVENT"));

        mockMvc.perform(delete("/notices/{noticeId}", noticeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
