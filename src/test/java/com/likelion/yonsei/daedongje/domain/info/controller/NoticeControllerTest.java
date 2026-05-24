package com.likelion.yonsei.daedongje.domain.info.controller;

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

import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private MockMvc mockMvc;

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
    void createNoticeWithImages_andReadFromList() throws Exception {
        String requestBody = """
                {
                  "title": "Notice title",
                  "content": "Notice content",
                  "instagramUrl": "https://instagram.com/notice",
                  "isPinned": true,
                  "category": "GENERAL",
                  "images": [
                    {
                      "image_url": "https://example.com/notice-1.png",
                      "display_order": 1
                    },
                    {
                      "image_url": "https://example.com/notice-2.png",
                      "display_order": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Notice title"))
                .andExpect(jsonPath("$.data.instagramUrl").value("https://instagram.com/notice"))
                .andExpect(jsonPath("$.data.hasImage").value(true))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/notice-1.png"))
                .andExpect(jsonPath("$.data.images", hasSize(2)))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/notice-1.png"))
                .andExpect(jsonPath("$.data.images[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data.images[1].imageUrl").value("https://example.com/notice-2.png"))
                .andExpect(jsonPath("$.data.images[1].displayOrder").value(2));

        var savedNotice = noticeRepository.findAll().get(0);
        String expectedDate = savedNotice.getUpdatedAt().toLocalDate().toString();
        String expectedTime = savedNotice.getUpdatedAt().toLocalTime().format(TIME_FORMATTER);

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].date").value(expectedDate))
                .andExpect(jsonPath("$.data[0].time").value(expectedTime))
                .andExpect(jsonPath("$.data[0].instagramUrl").value("https://instagram.com/notice"))
                .andExpect(jsonPath("$.data[0].images", hasSize(2)))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/notice-1.png"));
    }

    @Test
    void updateNotice_replacesImageList() throws Exception {
        String createBody = """
                {
                  "title": "Original title",
                  "content": "Original content",
                  "isPinned": false,
                  "images": [
                    {
                      "image_url": "https://example.com/original.png",
                      "display_order": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        Long noticeId = noticeRepository.findAll().get(0).getId();

        String updateBody = """
                {
                  "title": "Updated title",
                  "content": "Updated content",
                  "instagramUrl": "https://instagram.com/updated-notice",
                  "hasImage": true,
                  "isPinned": true,
                  "category": "EVENT",
                  "images": [
                    {
                      "image_url": "https://example.com/updated-1.png",
                      "display_order": 1
                    },
                    {
                      "image_url": "https://example.com/updated-2.png",
                      "display_order": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/admin/notices/{noticeId}", noticeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated title"))
                .andExpect(jsonPath("$.data.instagramUrl").value("https://instagram.com/updated-notice"))
                .andExpect(jsonPath("$.data.isPinned").value(true))
                .andExpect(jsonPath("$.data.category").value("EVENT"))
                .andExpect(jsonPath("$.data.images", hasSize(2)))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/updated-1.png"))
                .andExpect(jsonPath("$.data.images[1].imageUrl").value("https://example.com/updated-2.png"));

        mockMvc.perform(delete("/api/admin/notices/{noticeId}", noticeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void updateNotice_preservesExistingImages_whenImagesFieldIsOmitted() throws Exception {
        String createBody = """
                {
                  "title": "Original title",
                  "content": "Original content",
                  "isPinned": false,
                  "images": [
                    {
                      "image_url": "https://example.com/original.png",
                      "display_order": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        Long noticeId = noticeRepository.findAll().get(0).getId();

        String updateBody = """
                {
                  "title": "Updated title",
                  "content": "Updated content",
                  "instagramUrl": "https://instagram.com/kept-notice",
                  "hasImage": true,
                  "isPinned": true,
                  "category": "EVENT"
                }
                """;

        mockMvc.perform(put("/api/admin/notices/{noticeId}", noticeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.images", hasSize(1)))
                .andExpect(jsonPath("$.data.instagramUrl").value("https://instagram.com/kept-notice"))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/original.png"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/original.png"));
    }

    @Test
    void createNotice_rejectsDuplicateImageDisplayOrder() throws Exception {
        String requestBody = """
                {
                  "title": "Notice title",
                  "content": "Notice content",
                  "isPinned": false,
                  "images": [
                    {
                      "image_url": "https://example.com/notice-1.png",
                      "display_order": 1
                    },
                    {
                      "image_url": "https://example.com/notice-2.png",
                      "display_order": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminSessionMissing_rejectsNoticeWriteApi() throws Exception {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        String requestBody = """
                {
                  "title": "Unauthorized title",
                  "content": "Unauthorized content",
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
