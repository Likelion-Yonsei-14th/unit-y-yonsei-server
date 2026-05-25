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
import java.util.List;

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
    void 존재하지_않는_공연_id로_공지를_등록하면_검증_에러를_반환한다() throws Exception {
        String requestBody = """
                {
                  "title": "Notice title",
                  "content": "Notice content",
                  "hasImage": false,
                  "isPinned": false,
                  "category": "PERFORMANCE",
                  "performanceId": 999999
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("I-007"));
    }

    @Test
    void createNoticeWithImages_andReadFromList() throws Exception {
        String requestBody = """
                {
                  "title": "Notice title",
                  "content": "Notice content",
                  "instagramUrl": "https://instagram.com/notice",
                  "isPinned": true,
                  "category": "OTHERS",
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
                .andExpect(jsonPath("$.data.category").value("OTHERS"))
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
                .andExpect(jsonPath("$.data[0].noticeId").value(savedNotice.getId()))
                .andExpect(jsonPath("$.data[0].category").value("OTHERS"))
                .andExpect(jsonPath("$.data[0].date").value(expectedDate))
                .andExpect(jsonPath("$.data[0].time").value(expectedTime))
                .andExpect(jsonPath("$.data[0].instagramUrl").value("https://instagram.com/notice"))
                .andExpect(jsonPath("$.data[0].images", hasSize(2)))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/notice-1.png"));

        mockMvc.perform(get("/api/notices/{noticeId}", savedNotice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noticeId").value(savedNotice.getId()))
                .andExpect(jsonPath("$.data.title").value("Notice title"))
                .andExpect(jsonPath("$.data.content").value("Notice content"))
                .andExpect(jsonPath("$.data.category").value("OTHERS"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.images", hasSize(2)));
    }

    @Test
    void getNotices_filtersByCategory() throws Exception {
        String othersNotice = """
                {
                  "title": "Others notice",
                  "content": "Others content",
                  "isPinned": false,
                  "category": "OTHERS"
                }
                """;

        String boothNotice = """
                {
                  "title": "Booth notice",
                  "content": "Booth content",
                  "isPinned": false,
                  "category": "BOOTH"
                }
                """;

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(othersNotice))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(boothNotice))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notices").param("category", "OTHERS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Others notice"))
                .andExpect(jsonPath("$.data[0].category").value("OTHERS"));

        mockMvc.perform(get("/api/notices").param("category", "BOOTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Booth notice"))
                .andExpect(jsonPath("$.data[0].category").value("BOOTH"));
    }

    @Test
    void getNotices_acceptsAllNoticeCategories() throws Exception {
        for (String category : List.of("BLUERUN", "BOOTH", "PERFORMANCE", "OTHERS")) {
            String requestBody = """
                    {
                      "title": "%s notice",
                      "content": "%s content",
                      "isPinned": false,
                      "category": "%s"
                    }
                    """.formatted(category, category, category);

            mockMvc.perform(post("/api/admin/notices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.category").value(category));

            mockMvc.perform(get("/api/notices").param("category", category))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].category").value(category));
        }
    }

    @Test
    void getNotice_returnsNotFound_whenNoticeDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/notices/{noticeId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("I-001"));
    }

    @Test
    void getNotices_rejectsInvalidCategoryParameter() throws Exception {
        mockMvc.perform(get("/api/notices").param("category", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
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
                  "category": "BOOTH",
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
                .andExpect(jsonPath("$.data.category").value("BOOTH"))
                .andExpect(jsonPath("$.data.images", hasSize(2)))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/updated-1.png"))
                .andExpect(jsonPath("$.data.images[1].imageUrl").value("https://example.com/updated-2.png"));

        mockMvc.perform(delete("/api/admin/notices/{noticeId}", noticeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

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
                  "category": "PERFORMANCE"
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
