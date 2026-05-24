package com.likelion.yonsei.daedongje.domain.performance.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PerformanceReviewDashboardControllerTest {

    private static final String SUMMARY_URL = "/api/admin/performances/me/reviews/summary";
    private static final String REVIEWS_URL = "/api/admin/performances/me/reviews";

    @Autowired
    private MockMvc mockMvc;

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

    // ── Summary API ──────────────────────────────────────────────────────────

    @Test
    void summaryReturnsZeroVotesWhenNoMessages() throws Exception {
        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.performanceId").value(performance.getId()))
                .andExpect(jsonPath("$.data.performanceName").value("Main Stage"))
                .andExpect(jsonPath("$.data.totalVoteCount").value(0))
                .andExpect(jsonPath("$.data.favoriteStageResults").isEmpty());
    }

    @Test
    void summaryCountsVotesPerSetlistAndCalculatesVoteRate() throws Exception {
        PerformanceSetlist setlist1 = saveSetlist(performance, "여름밤의 꿈", "Band A", 1);
        PerformanceSetlist setlist2 = saveSetlist(performance, "비상", "Band A", 2);

        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, setlist1, "응원1"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, setlist1, "응원2"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, setlist2, "응원3"));

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVoteCount").value(3))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].setlistId").value(setlist1.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].songTitle").value("여름밤의 꿈"))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].voteCount").value(2))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].rank").value(1))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].setlistId").value(setlist2.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].voteCount").value(1))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].rank").value(2));
    }

    @Test
    void summaryExcludesMessagesWithNoSetlistFromVoteCount() throws Exception {
        PerformanceSetlist setlist = saveSetlist(performance, "여름밤의 꿈", "Band A", 1);
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, setlist, "투표 있음"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, null, "투표 없음"));

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVoteCount").value(1))
                .andExpect(jsonPath("$.data.favoriteStageResults.length()").value(1));
    }

    @Test
    void summaryRanksSortedByVoteCountDescThenSetlistIdAsc() throws Exception {
        PerformanceSetlist s1 = saveSetlist(performance, "Song A", "Band", 1);
        PerformanceSetlist s2 = saveSetlist(performance, "Song B", "Band", 2);
        PerformanceSetlist s3 = saveSetlist(performance, "Song C", "Band", 3);

        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s2, "v1"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s2, "v2"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s2, "v3"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s1, "v4"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s1, "v5"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s3, "v6"));

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favoriteStageResults[0].setlistId").value(s2.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[0].voteCount").value(3))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].setlistId").value(s1.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[1].voteCount").value(2))
                .andExpect(jsonPath("$.data.favoriteStageResults[2].setlistId").value(s3.getId()))
                .andExpect(jsonPath("$.data.favoriteStageResults[2].voteCount").value(1));
    }

    @Test
    void summaryReturnsNotFoundWhenNoPerformanceConnected() throws Exception {
        AdminUser superAdmin = adminUserRepository.save(adminUser("super-admin", AdminRole.SUPER));
        mockCurrentAdmin(superAdmin);

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void summaryReturnsForbiddenForBoothAdmin() throws Exception {
        AdminUser boothAdmin = adminUserRepository.save(adminUser("booth-admin", AdminRole.BOOTH));
        mockCurrentAdmin(boothAdmin);

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void summaryReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A-007"));
    }

    // ── Review List API ───────────────────────────────────────────────────────

    @Test
    void reviewsReturnsEmptyPageWhenNoMessages() throws Exception {
        mockMvc.perform(get(REVIEWS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void reviewsReturnsMessagesLatestFirst() throws Exception {
        PerformanceSetlist setlist = saveSetlist(performance, "여름밤의 꿈", "Band A", 1);
        PerformanceCheerMessage first = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, setlist, "첫 메시지"));
        PerformanceCheerMessage second = cheerMessageRepository.save(
                PerformanceCheerMessage.create(performance, null, "두 번째 메시지"));

        mockMvc.perform(get(REVIEWS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].reviewId").value(second.getId()))
                .andExpect(jsonPath("$.data.content[1].reviewId").value(first.getId()));
    }

    @Test
    void reviewsReturnsSetlistInfoWhenPresent() throws Exception {
        PerformanceSetlist setlist = saveSetlist(performance, "여름밤의 꿈", "Band A", 1);
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, setlist, "응원 메시지"));

        mockMvc.perform(get(REVIEWS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].setlistId").value(setlist.getId()))
                .andExpect(jsonPath("$.data.content[0].songTitle").value("여름밤의 꿈"))
                .andExpect(jsonPath("$.data.content[0].message").value("응원 메시지"));
    }

    @Test
    void reviewsPaginationWorks() throws Exception {
        for (int i = 0; i < 15; i++) {
            cheerMessageRepository.save(PerformanceCheerMessage.create(performance, null, "메시지 " + i));
        }

        mockMvc.perform(get(REVIEWS_URL).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(15))
                .andExpect(jsonPath("$.data.content.length()").value(10))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get(REVIEWS_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void reviewsFiltersbySetlistId() throws Exception {
        PerformanceSetlist s1 = saveSetlist(performance, "Song A", "Band", 1);
        PerformanceSetlist s2 = saveSetlist(performance, "Song B", "Band", 2);
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s1, "s1 메시지"));
        cheerMessageRepository.save(PerformanceCheerMessage.create(performance, s2, "s2 메시지"));

        mockMvc.perform(get(REVIEWS_URL).param("setlistId", String.valueOf(s1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].setlistId").value(s1.getId()));
    }

    @Test
    void reviewsReturnsForbiddenWhenSetlistBelongsToAnotherPerformance() throws Exception {
        AdminUser otherAdmin = adminUserRepository.save(adminUser("other-performer", AdminRole.PERFORMER));
        Performance otherPerformance = performanceRepository.save(Performance.create(otherAdmin, "Other Stage"));
        PerformanceSetlist otherSetlist = saveSetlist(otherPerformance, "Other Song", "Other Band", 1);

        mockMvc.perform(get(REVIEWS_URL).param("setlistId", String.valueOf(otherSetlist.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PCM-005"));
    }

    @Test
    void reviewsReturnsNotFoundWhenSetlistDoesNotExist() throws Exception {
        mockMvc.perform(get(REVIEWS_URL).param("setlistId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PS-001"));
    }

    @Test
    void reviewsReturnsForbiddenForBoothAdmin() throws Exception {
        AdminUser boothAdmin = adminUserRepository.save(adminUser("booth-admin", AdminRole.BOOTH));
        mockCurrentAdmin(boothAdmin);

        mockMvc.perform(get(REVIEWS_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void reviewsReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(AuthErrorCode.UNAUTHORIZED));

        mockMvc.perform(get(REVIEWS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A-007"));
    }

    @Test
    void reviewsDoesNotFailOnInvalidPaginationParams() throws Exception {
        // page<0, size<1 은 PageRequest.of 에서 IllegalArgumentException → 500 이 되던 케이스.
        // 방어 처리 후 500 이 아니라 정상 200 이어야 한다.
        mockMvc.perform(get(REVIEWS_URL).param("page", "-1").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Swagger 노출 확인 ─────────────────────────────────────────────────────

    @Test
    void openApiExposesReviewDashboardApis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + SUMMARY_URL + "'].get").exists())
                .andExpect(jsonPath("$.paths['" + REVIEWS_URL + "'].get").exists());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PerformanceSetlist saveSetlist(Performance perf, String songTitle, String singerName, int order) {
        return performanceSetlistRepository.save(PerformanceSetlist.create(perf, songTitle, singerName, order, null));
    }

    private AdminUser adminUser(String loginId, AdminRole role) {
        return AdminUser.create(loginId, "password-hash", "Performance Team", role, "Representative", "010-0000-0000", null);
    }

    private void mockCurrentAdmin(AdminUser adminUser) {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(adminUser.getId(), adminUser.getRole(), adminUser.getLoginId()));
    }
}
