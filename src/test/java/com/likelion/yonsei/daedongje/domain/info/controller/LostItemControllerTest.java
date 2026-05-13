package com.likelion.yonsei.daedongje.domain.info.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.yonsei.daedongje.domain.info.repository.LostItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class LostItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LostItemRepository lostItemRepository;

    @BeforeEach
    void setUp() {
        lostItemRepository.deleteAll();
    }

    @Test
    void 분실물을_등록하고_목록에서_조회할_수_있다() throws Exception {
        String requestBody = """
                {
                  "name": "학생증",
                  "location": "중앙 무대 앞",
                  "description": "파란색 케이스가 달린 학생증입니다.",
                  "has_image": true,
                  "status": "STORED"
                }
                """;

        mockMvc.perform(post("/api/lost-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("학생증"))
                .andExpect(jsonPath("$.data.location").value("중앙 무대 앞"))
                .andExpect(jsonPath("$.data.has_image").value(true))
                .andExpect(jsonPath("$.data.image_url").value("pending-upload"))
                .andExpect(jsonPath("$.data.status").value("STORED"));

        mockMvc.perform(get("/api/lost-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("학생증"));
    }

    @Test
    void 분실물을_수정하고_삭제할_수_있다() throws Exception {
        String createBody = """
                {
                  "name": "지갑",
                  "location": "노천극장 입구",
                  "description": "검은색 반지갑"
                }
                """;

        String response = mockMvc.perform(post("/api/lost-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long lostItemId = objectMapper.readTree(response).path("data").path("id").asLong();

        String updateBody = """
                {
                  "name": "지갑",
                  "location": "학생회관 안내데스크",
                  "description": "검은색 반지갑, 신분증 포함",
                  "image_url": "https://example.com/lost-item.png",
                  "status": "CLAIMED"
                }
                """;

        mockMvc.perform(put("/api/lost-items/{lostItemId}", lostItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.location").value("학생회관 안내데스크"))
                .andExpect(jsonPath("$.data.image_url").value("https://example.com/lost-item.png"))
                .andExpect(jsonPath("$.data.status").value("CLAIMED"));

        mockMvc.perform(delete("/api/lost-items/{lostItemId}", lostItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/lost-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
