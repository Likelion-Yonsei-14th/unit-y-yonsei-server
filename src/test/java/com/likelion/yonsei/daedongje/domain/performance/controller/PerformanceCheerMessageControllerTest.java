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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private static final String ADMIN_REVIEW_SUMMARY_URL = "/api/admin/performances/me/reviews/summary";
    private static final String ADMIN_REVIEWS_URL = "/api/admin/performances/me/reviews";
    private static final String ADMIN_ALL_CHEER_MESSAGES_URL = "/api/admin/performances/cheer-messages";

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void createCheerMessageIsPublicAndDoesNotRequireNickname() throws Exception {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        String requestBody = """
                {
                  "message": "public cheer"
                }
                """;

        mockMvc.perform(post(USER_CHEER_MESSAGES_URL, performance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("public cheer"));

        assertThat(cheerMessageRepository.count()).isEqualTo(1);
    }

    @Test
    void performanceCheerMessagesTableDoesNotHaveNicknameColumn() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_NAME = 'PERFORMANCE_CHEER_MESSAGES'
                          AND COLUMN_NAME = 'NICKNAME'
                        """,
                Integer.class
        );

        assertThat(columnCount).isZero();
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

        mockMvc.perform(get(ADMIN_REVIEWS_URL)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

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
    void getMyPerformanceReviewSummaryReturnsFavoriteStageResults() throws Exception {
        PerformanceSetlist firstSetlist = saveSetlist(performance, "First Song", "Main Band", 1);
        PerformanceSetlist secondSetlist = saveSetlist(performance, "Second Song", "Main Band", 2);
        PerformanceSetlist thirdSetlist = saveSetlist(performance, "Third Song", "Main Band", 3);

        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, secondSetlist, "second 1"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, secondSetlist, "second 2"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, firstSetlist, "first 1"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, firstSetlist, "first 2"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, thirdSetlist, "third 1"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, null, "no vote"));
        PerformanceCheerMessage hidden = PerformanceCheerMessage.create(performance, thirdSetlist, "hidden");
        hidden.hide();
        cheerMessageRepository.save(hidden);

        mockMvc.perform(get(ADMIN_REVIEW_SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.performanceId").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Main Stage"))
                .andExpect(jsonPath("$.data.totalVoteCount").value(5))
                .andExpect(jsonPath("$.data.favoriteStageResults", hasSize(3)))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].rank").value(1))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].setlistId").value(firstSetlist.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].songTitle").value("First Song"))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].voteCount").value(2))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].voteRate").value(40.0))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].rank").value(2))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].setlistId").value(secondSetlist.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].voteCount").value(2))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].voteRate").value(40.0))
                .andExpect(jsonPath("$.data.favoriteStageResults[2].rank").value(3))
                .andExpect(jsonPath("$.data.favoriteStageResults[2].setlistId").value(thirdSetlist.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[2].voteCount").value(1))
                .andExpect(jsonPath("$.data.favoriteStageResults[2].voteRate").value(20.0))
                .andExpect(jsonPath("$.data.nickname").doesNotExist());
    }

    @Test
    void getMyPerformanceReviewSummaryReturnsEmptyWhenNoReviews() throws Exception {
        mockMvc.perform(get(ADMIN_REVIEW_SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalVoteCount").value(0))
                .andExpect(jsonPath("$.data.favoriteStageResults", hasSize(0)));
    }

    @Test
    void getMyPerformanceReviewSummaryReturnsNotFoundWhenNoPerformanceIsConnected() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("review-super-admin", AdminRole.SUPER));
        mockCurrentAdmin(superAdmin);

        mockMvc.perform(get(ADMIN_REVIEW_SUMMARY_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getMyPerformanceReviewsReturnsPagedReviewsOrderedByLatest() throws Exception {
        PerformanceSetlist setlist = saveSetlist(performance, "Blue Spring", "Main Band", 1);
        PerformanceCheerMessage first = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, setlist, "old review")
        );
        PerformanceCheerMessage second = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, setlist, "new review")
        );
        PerformanceCheerMessage third = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, null, "no setlist review")
        );
        PerformanceCheerMessage hidden = PerformanceCheerMessage.create(performance, setlist, "hidden review");
        hidden.hide();
        cheerMessageRepository.save(hidden);

        mockMvc.perform(get(ADMIN_REVIEWS_URL)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].reviewId").value(third.getId()))
                .andExpect(jsonPath("$.data.content[0].nickname").value(audienceNickname(third.getId())))
                .andExpect(jsonPath("$.data.content[0].setlistId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].message").value("no setlist review"))
                .andExpect(jsonPath("$.data.content[1].reviewId").value(second.getId()))
                .andExpect(jsonPath("$.data.content[1].nickname").value(audienceNickname(second.getId())))
                .andExpect(jsonPath("$.data.content[1].setlistId").value(setlist.getId()))
                .andExpect(jsonPath("$.data.content[1].songTitle").value("Blue Spring"))
                .andExpect(jsonPath("$.data.content[1].singerName").value("Main Band"))
                .andExpect(jsonPath("$.data.content[1].songOrder").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        assertThat(first.getId()).isNotNull();
    }

    @Test
    void getMyPerformanceReviewsFiltersBySetlistId() throws Exception {
        PerformanceSetlist firstSetlist = saveSetlist(performance, "First Song", "Main Band", 1);
        PerformanceSetlist secondSetlist = saveSetlist(performance, "Second Song", "Main Band", 2);
        PerformanceCheerMessage firstReview = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, firstSetlist, "first review")
        );
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, secondSetlist, "second review"));

        mockMvc.perform(get(ADMIN_REVIEWS_URL)
                        .param("setlistId", String.valueOf(firstSetlist.getId()))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].nickname").value(audienceNickname(firstReview.getId())))
                .andExpect(jsonPath("$.data.content[0].setlistId").value(firstSetlist.getId()))
                .andExpect(jsonPath("$.data.content[0].message").value("first review"));
    }

    @Test
    void getMyPerformanceReviewsReturnsNotFoundWhenSetlistDoesNotExist() throws Exception {
        mockMvc.perform(get(ADMIN_REVIEWS_URL)
                        .param("setlistId", "999999")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PS-001"));
    }

    @Test
    void getMyPerformanceReviewsReturnsForbiddenWhenSetlistBelongsToAnotherPerformance() throws Exception {
        Performance otherPerformance = saveOtherPerformance();
        PerformanceSetlist otherSetlist = saveSetlist(otherPerformance, "Other Song", "Other Band", 1);

        mockMvc.perform(get(ADMIN_REVIEWS_URL)
                        .param("setlistId", String.valueOf(otherSetlist.getId()))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void getMyPerformanceReviewsReturnsEmptyPageWhenNoReviews() throws Exception {
        mockMvc.perform(get(ADMIN_REVIEWS_URL)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void getAllCheerMessagesReturnsAllPerformancesAndStatusesForSuper() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("all-super-admin", AdminRole.SUPER));
        mockCurrentAdmin(superAdmin);

        Performance other = saveOtherPerformance();
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, null, "메인 응원"));
        PerformanceCheerMessage hidden = PerformanceCheerMessage.create(other, null, "다른공연 숨김");
        hidden.hide();
        cheerMessageRepository.save(hidden);

        mockMvc.perform(get(ADMIN_ALL_CHEER_MESSAGES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].message").value("메인 응원"))
                .andExpect(jsonPath("$.data[0].displayStatus").value("VISIBLE"))
                .andExpect(jsonPath("$.data[1].message").value("다른공연 숨김"))
                .andExpect(jsonPath("$.data[1].displayStatus").value("HIDDEN"));
    }

    @Test
    void getAllCheerMessagesAllowedForMaster() throws Exception {
        AdminUser masterAdmin = adminUserRepository.save(adminUser("all-master-admin", AdminRole.MASTER));
        mockCurrentAdmin(masterAdmin);
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, null, "응원"));

        mockMvc.perform(get(ADMIN_ALL_CHEER_MESSAGES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void getAllCheerMessagesRejectsPerformer() throws Exception {
        // setUp 의 performerAdmin(PERFORMER) 으로 호출 — 전체 조회는 SUPER/MASTER 전용.
        mockMvc.perform(get(ADMIN_ALL_CHEER_MESSAGES_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void openApiExposesCheerMessageApisOnlyForRequestedPaths() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = apiDocs.path("paths");
        String apiDocsJson = apiDocs.toString();

        assertThat(apiDocs.path("tags").toString()).contains("공연 응원 메시지");
        assertThat(apiDocsJson).doesNotContain("currentAdmin");
        assertThat(apiDocsJson).doesNotContain("Performance Cheer Message");
        assertThat(apiDocsJson).doesNotContain("Create performance cheer message");
        assertThat(apiDocsJson).doesNotContain("Get performance review collection");
        assertThat(apiDocsJson).doesNotContain("Get visible performance cheer messages");
        assertThat(apiDocsJson).doesNotContain("Get all performance cheer messages");
        assertThat(apiDocsJson).doesNotContain("SUPER/MASTER");
        assertThat(paths.path("/api/performances/{id}/cheer-messages").has("post")).isTrue();
        assertThat(paths.path("/api/performances/{id}/cheer-messages").has("get")).isFalse();
        assertThat(paths.path(ADMIN_CHEER_MESSAGES_URL).has("get")).isTrue();
        assertThat(paths.path(ADMIN_REVIEW_SUMMARY_URL).has("get")).isTrue();
        assertThat(paths.path(ADMIN_REVIEWS_URL).has("get")).isTrue();
        assertThat(paths.path(ADMIN_ALL_CHEER_MESSAGES_URL).has("get")).isTrue();
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

    private String audienceNickname(Long id) {
        return "관객 " + String.format("%05d", id);
    }
}
