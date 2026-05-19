package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceCheerMessageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceSetlistRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PerformanceCheerMessageControllerTest {

    private static final String USER_CHEER_MESSAGES_URL = "/api/performances/{id}/cheer-messages";
    private static final String ADMIN_CHEER_MESSAGES_URL = "/api/admin/performances/me/cheer-messages";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private PerformanceSetlistRepository performanceSetlistRepository;

    @Autowired
    private PerformanceCheerMessageRepository cheerMessageRepository;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    private AdminUser performerAdmin;
    private Performance performance;

    @BeforeEach
    void setUp() {
        cheerMessageRepository.deleteAll();
        performanceSetlistRepository.deleteAll();
        performanceRepository.deleteAll();
        adminUserRepository.deleteAll();
        Mockito.reset(adminAuthContextService);

        performerAdmin = adminUserRepository.save(adminUser("performer-admin", AdminRole.PERFORMER));
        performance = performanceRepository.save(Performance.create(performerAdmin, "Main Stage"));

        mockCurrentAdmin(performerAdmin);
    }

    @Test
    void createCheerMessageReturnsCreatedWithoutSetlist() throws Exception {
        String requestBody = """
                {
                  "message": "응원합니다!"
                }
                """;

        mockMvc.perform(post(USER_CHEER_MESSAGES_URL, performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.performanceId").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Main Stage"))
                .andExpect(jsonPath("$.data.setlistId").doesNotExist())
                .andExpect(jsonPath("$.data.message").value("응원합니다!"))
                .andExpect(jsonPath("$.data.displayStatus").value("VISIBLE"));

        assertThat(cheerMessageRepository.count()).isEqualTo(1);
    }

    @Test
    void createCheerMessageReturnsCreatedWithSetlist() throws Exception {
        PerformanceSetlist setlist = saveSetlist(performance, "Blue Spring", "Main Band", 1);
        String requestBody = """
                {
                  "setlistId": %d,
                  "message": "이 곡 좋아요!"
                }
                """.formatted(setlist.getId());

        mockMvc.perform(post(USER_CHEER_MESSAGES_URL, performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.setlistId").value(setlist.getId()))
                .andExpect(jsonPath("$.data.songTitle").value("Blue Spring"))
                .andExpect(jsonPath("$.data.singerName").value("Main Band"))
                .andExpect(jsonPath("$.data.message").value("이 곡 좋아요!"));
    }

    @Test
    void createCheerMessageReturnsBadRequestWhenMessageIsBlank() throws Exception {
        String requestBody = """
                {
                  "message": "   "
                }
                """;

        mockMvc.perform(post(USER_CHEER_MESSAGES_URL, performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createCheerMessageReturnsNotFoundWhenPerformanceDoesNotExist() throws Exception {
        String requestBody = """
                {
                  "message": "응원합니다!"
                }
                """;

        mockMvc.perform(post(USER_CHEER_MESSAGES_URL, 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void createCheerMessageReturnsNotFoundWhenSetlistDoesNotExist() throws Exception {
        String requestBody = """
                {
                  "setlistId": 999999,
                  "message": "응원합니다!"
                }
                """;

        mockMvc.perform(post(USER_CHEER_MESSAGES_URL, performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PS-001"));
    }

    @Test
    void createCheerMessageReturnsForbiddenWhenSetlistBelongsToAnotherPerformance() throws Exception {
        Performance otherPerformance = saveOtherPerformance();
        PerformanceSetlist otherSetlist = saveSetlist(otherPerformance, "Other Song", "Other Band", 1);
        String requestBody = """
                {
                  "setlistId": %d,
                  "message": "응원합니다!"
                }
                """.formatted(otherSetlist.getId());

        mockMvc.perform(post(USER_CHEER_MESSAGES_URL, performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PCM-005"));
    }

    @Test
    void getCheerMessagesReturnsVisibleMessagesOrderedByCreatedAtAscAndIdAsc() throws Exception {
        PerformanceCheerMessage first = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, null, "첫 번째 응원")
        );
        PerformanceCheerMessage second = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, null, "두 번째 응원")
        );

        mockMvc.perform(get(USER_CHEER_MESSAGES_URL, performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(first.getId()))
                .andExpect(jsonPath("$.data[0].message").value("첫 번째 응원"))
                .andExpect(jsonPath("$.data[1].id").value(second.getId()))
                .andExpect(jsonPath("$.data[1].message").value("두 번째 응원"));
    }

    @Test
    void getCheerMessagesReturnsNotFoundWhenPerformanceDoesNotExist() throws Exception {
        mockMvc.perform(get(USER_CHEER_MESSAGES_URL, 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getCheerMessagesExcludesHiddenMessages() throws Exception {
        PerformanceCheerMessage visible = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, null, "보이는 응원")
        );
        PerformanceCheerMessage hidden = PerformanceCheerMessage.create(performance, null, "숨김 응원");
        hidden.hide();
        cheerMessageRepository.save(hidden);

        mockMvc.perform(get(USER_CHEER_MESSAGES_URL, performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(visible.getId()))
                .andExpect(jsonPath("$.data[0].message").value("보이는 응원"));
    }

    @Test
    void getMyPerformanceCheerMessagesReturnsAllStatuses() throws Exception {
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, null, "보이는 응원"));
        PerformanceCheerMessage hidden = PerformanceCheerMessage.create(performance, null, "숨김 응원");
        hidden.hide();
        cheerMessageRepository.save(hidden);

        mockMvc.perform(get(ADMIN_CHEER_MESSAGES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].displayStatus").value("VISIBLE"))
                .andExpect(jsonPath("$.data[1].displayStatus").value("HIDDEN"));
    }

    @Test
    void deleteMyPerformanceCheerMessageHidesMessage() throws Exception {
        PerformanceCheerMessage cheerMessage = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, null, "삭제할 응원")
        );

        mockMvc.perform(delete(ADMIN_CHEER_MESSAGES_URL + "/{messageId}", cheerMessage.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(USER_CHEER_MESSAGES_URL, performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        PerformanceCheerMessage saved = cheerMessageRepository.findById(cheerMessage.getId()).orElseThrow();
        assertThat(saved.getDisplayStatus().name()).isEqualTo("HIDDEN");
    }

    @Test
    void deleteMyPerformanceCheerMessageReturnsForbiddenWhenMessageBelongsToAnotherPerformance() throws Exception {
        Performance otherPerformance = saveOtherPerformance();
        PerformanceCheerMessage otherMessage = cheerMessageRepository.save(
                PerformanceCheerMessage.create(otherPerformance, null, "다른 공연 응원")
        );

        mockMvc.perform(delete(ADMIN_CHEER_MESSAGES_URL + "/{messageId}", otherMessage.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void boothAdminAccessIsRejected() throws Exception {
        AdminUser boothAdmin = adminUserRepository.save(adminUser("booth-admin", AdminRole.BOOTH));
        mockCurrentAdmin(boothAdmin);

        mockMvc.perform(get(ADMIN_CHEER_MESSAGES_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void masterAdminAccessIsRejected() throws Exception {
        AdminUser masterAdmin = adminUserRepository.save(adminUser("master-admin", AdminRole.MASTER));
        mockCurrentAdmin(masterAdmin);

        mockMvc.perform(get(ADMIN_CHEER_MESSAGES_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void superAdminAccessReturnsNotFoundWhenNoPerformanceIsConnected() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("super-admin", AdminRole.SUPER));
        mockCurrentAdmin(superAdmin);

        mockMvc.perform(get(ADMIN_CHEER_MESSAGES_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void adminApiReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        mockMvc.perform(get(ADMIN_CHEER_MESSAGES_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A-007"));
    }

    @Test
    void openApiExposesCheerMessageApisOnlyForRequestedPaths() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = apiDocs.path("paths");

        assertThat(apiDocs.path("tags").toString()).contains("공연 응원 메시지");
        assertThat(paths.path("/api/performances/{id}/cheer-messages").has("post")).isTrue();
        assertThat(paths.path("/api/performances/{id}/cheer-messages").has("get")).isTrue();
        assertThat(paths.path(ADMIN_CHEER_MESSAGES_URL).has("get")).isTrue();
        assertThat(paths.path(ADMIN_CHEER_MESSAGES_URL + "/{messageId}").has("delete")).isTrue();
        assertThat(paths.has("/api/admin/performances/{id}/cheer-messages")).isFalse();
        assertThat(paths.path("/api/admin/performances").has("post")).isFalse();
        assertThat(paths.path("/performances").has("post")).isFalse();
    }

    private PerformanceSetlist saveSetlist(
            Performance performance,
            String songTitle,
            String singerName,
            Integer songOrder
    ) {
        return performanceSetlistRepository.save(PerformanceSetlist.create(
                performance,
                songTitle,
                singerName,
                songOrder,
                null
        ));
    }

    private Performance saveOtherPerformance() {
        AdminUser otherAdmin = adminUserRepository.save(adminUser("other-performer-admin", AdminRole.PERFORMER));
        return performanceRepository.save(Performance.create(otherAdmin, "Other Stage"));
    }

    private AdminUser adminUser(String loginId, AdminRole role) {
        return AdminUser.create(
                loginId,
                "password-hash",
                "Performance Team",
                role,
                "Representative",
                "010-0000-0000",
                null
        );
    }

    private void mockCurrentAdmin(AdminUser adminUser) {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(adminUser.getId(), adminUser.getRole(), adminUser.getLoginId()));
    }
}
