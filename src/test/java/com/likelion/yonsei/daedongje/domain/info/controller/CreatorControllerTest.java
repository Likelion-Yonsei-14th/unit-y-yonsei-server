package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.info.entity.Creator;
import com.likelion.yonsei.daedongje.domain.info.repository.CreatorRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @MockBean
    private AdminAuthContextService adminAuthContextService;

    @BeforeEach
    void setUp() {
        creatorRepository.deleteAll();
        when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(1L, AdminRole.MASTER, "creator-admin"));
    }

    @Test
    void getCreators_returnsOrderedList() throws Exception {
        creatorRepository.save(Creator.create(
                "Design",
                "Visual Design",
                "20201234",
                "Designer Kim",
                2
        ));
        creatorRepository.save(Creator.create(
                "Backend",
                "Computer Science",
                "20191234",
                "Backend Park",
                1
        ));

        mockMvc.perform(get("/api/creators")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].part_name").value("Backend"))
                .andExpect(jsonPath("$.data[0].department_name").value("Computer Science"))
                .andExpect(jsonPath("$.data[0].student_id").value("20191234"))
                .andExpect(jsonPath("$.data[1].part_name").value("Design"));
    }

    @Test
    void createUpdateDeleteCreator_works() throws Exception {
        String createRequest = """
                {
                  "part_name": "Frontend",
                  "department_name": "Computer Science",
                  "student_id": "20201234",
                  "name": "Frontend Choi",
                  "display_order": 3
                }
                """;

        mockMvc.perform(post("/api/admin/creators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.part_name").value("Frontend"));

        Long id = creatorRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/creators/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Frontend Choi"));

        String updateRequest = """
                {
                  "part_name": "Product",
                  "department_name": "Industrial Engineering",
                  "student_id": "20209999",
                  "name": "Product Jung",
                  "display_order": 1
                }
                """;

        mockMvc.perform(put("/api/admin/creators/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.part_name").value("Product"))
                .andExpect(jsonPath("$.data.display_order").value(1));

        mockMvc.perform(delete("/api/admin/creators/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void createCreator_requiresAuthentication() throws Exception {
        Mockito.reset(adminAuthContextService);
        when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        String createRequest = """
                {
                  "part_name": "Frontend",
                  "department_name": "Computer Science",
                  "student_id": "20201234",
                  "name": "Frontend Choi",
                  "display_order": 3
                }
                """;

        mockMvc.perform(post("/api/admin/creators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isUnauthorized());
    }
}
