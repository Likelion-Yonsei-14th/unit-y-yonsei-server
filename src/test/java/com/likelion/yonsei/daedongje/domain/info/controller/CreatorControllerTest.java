package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.domain.info.entity.Creator;
import com.likelion.yonsei.daedongje.domain.info.repository.CreatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CreatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreatorRepository creatorRepository;

    @BeforeEach
    void setUp() {
        creatorRepository.deleteAll();
    }

    @Test
    void getCreators_returnsOrderedList() throws Exception {
        creatorRepository.save(Creator.create(
                "Design",
                "시각디자인학과",
                "20201234",
                "김디자인",
                2
        ));
        creatorRepository.save(Creator.create(
                "Backend",
                "컴퓨터과학과",
                "20191234",
                "박백엔드",
                1
        ));

        mockMvc.perform(get("/api/creators")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].part_name").value("Backend"))
                .andExpect(jsonPath("$.data[0].department_name").value("컴퓨터과학과"))
                .andExpect(jsonPath("$.data[0].student_id").value("20191234"))
                .andExpect(jsonPath("$.data[1].part_name").value("Design"));
    }

    @Test
    void createUpdateDeleteCreator_works() throws Exception {
        String createRequest = """
                {
                  "part_name": "Frontend",
                  "department_name": "컴퓨터과학과",
                  "student_id": "20201234",
                  "name": "최프론트",
                  "display_order": 3
                }
                """;

        String response = mockMvc.perform(post("/api/creators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.part_name").value("Frontend"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = creatorRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/creators/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("최프론트"));

        String updateRequest = """
                {
                  "part_name": "Product",
                  "department_name": "산업공학과",
                  "student_id": "20209999",
                  "name": "정프로덕트",
                  "display_order": 1
                }
                """;

        mockMvc.perform(put("/api/creators/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.part_name").value("Product"))
                .andExpect(jsonPath("$.data.display_order").value(1));

        mockMvc.perform(delete("/api/creators/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true));
    }
}
