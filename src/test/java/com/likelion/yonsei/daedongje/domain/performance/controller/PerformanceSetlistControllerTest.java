package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PerformanceSetlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private PerformanceSetlistRepository performanceSetlistRepository;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    private AdminUser adminUser;
    private Performance performance;

    @BeforeEach
    void setUp() {
        performanceSetlistRepository.deleteAll();
        performanceRepository.deleteAll();
        adminUserRepository.deleteAll();
        Mockito.reset(adminAuthContextService);

        adminUser = adminUserRepository.save(createAdminUser("performer-admin"));
        performance = performanceRepository.save(Performance.create(adminUser, "Main Stage"));

        mockCurrentAdmin(adminUser);
    }

    @Test
    void createMyPerformanceSetlistReturnsCreated() throws Exception {
        String requestBody = """
                {
                  "songTitle": "Blue Spring",
                  "singerName": "Main Band",
                  "songOrder": 1,
                  "note": "Opening"
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/setlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.performanceId").value(performance.getId()))
                .andExpect(jsonPath("$.data.songTitle").value("Blue Spring"))
                .andExpect(jsonPath("$.data.singerName").value("Main Band"))
                .andExpect(jsonPath("$.data.songOrder").value(1))
                .andExpect(jsonPath("$.data.note").value("Opening"));
    }

    @Test
    void createMyPerformanceSetlistReturnsBadRequestWhenSongTitleIsBlank() throws Exception {
        String requestBody = """
                {
                  "songTitle": "   ",
                  "singerName": "Main Band",
                  "songOrder": 1,
                  "note": null
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/setlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceSetlistReturnsBadRequestWhenSingerNameIsBlank() throws Exception {
        String requestBody = """
                {
                  "songTitle": "Blue Spring",
                  "singerName": "   ",
                  "songOrder": 1,
                  "note": null
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/setlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceSetlistReturnsBadRequestWhenSongOrderIsNull() throws Exception {
        String requestBody = """
                {
                  "songTitle": "Blue Spring",
                  "singerName": "Main Band",
                  "songOrder": null,
                  "note": null
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/setlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceSetlistReturnsBadRequestWhenSongOrderIsZero() throws Exception {
        String requestBody = """
                {
                  "songTitle": "Blue Spring",
                  "singerName": "Main Band",
                  "songOrder": 0,
                  "note": null
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/setlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceSetlistReturnsNotFoundWhenCurrentAdminHasNoPerformance() throws Exception {
        AdminUser adminWithoutPerformance = adminUserRepository.save(createAdminUser("admin-without-performance"));
        mockCurrentAdmin(adminWithoutPerformance);

        String requestBody = """
                {
                  "songTitle": "Blue Spring",
                  "singerName": "Main Band",
                  "songOrder": 1,
                  "note": null
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/setlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void updateMyPerformanceSetlistReturnsOkAndUpdatesProvidedFieldsOnly() throws Exception {
        PerformanceSetlist setlist = performanceSetlistRepository.save(PerformanceSetlist.create(
                performance,
                "Blue Spring",
                "Main Band",
                1,
                "Opening"
        ));

        String requestBody = """
                {
                  "songTitle": "Updated Song",
                  "singerName": null,
                  "songOrder": 2,
                  "note": null
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", setlist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.songTitle").value("Updated Song"))
                .andExpect(jsonPath("$.data.singerName").value("Main Band"))
                .andExpect(jsonPath("$.data.songOrder").value(2))
                .andExpect(jsonPath("$.data.note").value("Opening"));
    }

    @Test
    void updateMyPerformanceSetlistAllowsEmptyNote() throws Exception {
        PerformanceSetlist setlist = performanceSetlistRepository.save(PerformanceSetlist.create(
                performance,
                "Blue Spring",
                "Main Band",
                1,
                "Opening"
        ));

        String requestBody = """
                {
                  "note": ""
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", setlist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.songTitle").value("Blue Spring"))
                .andExpect(jsonPath("$.data.singerName").value("Main Band"))
                .andExpect(jsonPath("$.data.songOrder").value(1))
                .andExpect(jsonPath("$.data.note").value(""));
    }

    @Test
    void updateMyPerformanceSetlistReturnsBadRequestWhenSongTitleIsBlank() throws Exception {
        PerformanceSetlist setlist = saveMySetlist();
        String requestBody = """
                {
                  "songTitle": "   "
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", setlist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateMyPerformanceSetlistReturnsBadRequestWhenSingerNameIsBlank() throws Exception {
        PerformanceSetlist setlist = saveMySetlist();
        String requestBody = """
                {
                  "singerName": "   "
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", setlist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateMyPerformanceSetlistReturnsBadRequestWhenSongOrderIsZero() throws Exception {
        PerformanceSetlist setlist = saveMySetlist();
        String requestBody = """
                {
                  "songOrder": 0
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", setlist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateMyPerformanceSetlistReturnsNotFoundWhenSetlistDoesNotExist() throws Exception {
        String requestBody = """
                {
                  "songTitle": "Updated Song"
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PS-001"));
    }

    @Test
    void updateMyPerformanceSetlistReturnsForbiddenWhenSetlistBelongsToAnotherPerformance() throws Exception {
        PerformanceSetlist otherSetlist = saveOtherPerformanceSetlist();
        String requestBody = """
                {
                  "songTitle": "Updated Song"
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", otherSetlist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void updateMyPerformanceSetlistReturnsNotFoundWhenCurrentAdminHasNoPerformance() throws Exception {
        PerformanceSetlist setlist = saveMySetlist();
        AdminUser adminWithoutPerformance = adminUserRepository.save(createAdminUser("admin-without-performance"));
        mockCurrentAdmin(adminWithoutPerformance);
        String requestBody = """
                {
                  "songTitle": "Updated Song"
                }
                """;

        mockMvc.perform(patch("/api/admin/performances/me/setlists/{setlistId}", setlist.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void deleteMyPerformanceSetlistReturnsNoContent() throws Exception {
        PerformanceSetlist setlist = saveMySetlist();

        mockMvc.perform(delete("/api/admin/performances/me/setlists/{setlistId}", setlist.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMyPerformanceSetlistReturnsNotFoundWhenSetlistDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/admin/performances/me/setlists/{setlistId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PS-001"));
    }

    @Test
    void deleteMyPerformanceSetlistReturnsForbiddenWhenSetlistBelongsToAnotherPerformance() throws Exception {
        PerformanceSetlist otherSetlist = saveOtherPerformanceSetlist();

        mockMvc.perform(delete("/api/admin/performances/me/setlists/{setlistId}", otherSetlist.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A-009"));
    }

    @Test
    void deleteMyPerformanceSetlistReturnsNotFoundWhenCurrentAdminHasNoPerformance() throws Exception {
        PerformanceSetlist setlist = saveMySetlist();
        AdminUser adminWithoutPerformance = adminUserRepository.save(createAdminUser("admin-without-performance"));
        mockCurrentAdmin(adminWithoutPerformance);

        mockMvc.perform(delete("/api/admin/performances/me/setlists/{setlistId}", setlist.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getPerformanceSetlistsReturnsOrderedSetlists() throws Exception {
        performanceSetlistRepository.save(PerformanceSetlist.create(
                performance,
                "Third Song",
                "Main Band",
                2,
                null
        ));
        PerformanceSetlist first = performanceSetlistRepository.save(PerformanceSetlist.create(
                performance,
                "First Song",
                "Main Band",
                1,
                null
        ));
        PerformanceSetlist sameOrderSecond = performanceSetlistRepository.save(PerformanceSetlist.create(
                performance,
                "Second Song",
                "Main Band",
                1,
                null
        ));

        mockMvc.perform(get("/performances/{id}/setlists", performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").value(first.getId()))
                .andExpect(jsonPath("$.data[1].id").value(sameOrderSecond.getId()))
                .andExpect(jsonPath("$.data[2].songOrder").value(2));
    }

    @Test
    void getPerformanceSetlistsReturnsEmptyArrayWhenPerformanceHasNoSetlists() throws Exception {
        mockMvc.perform(get("/performances/{id}/setlists", performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getPerformanceSetlistsReturnsNotFoundWhenPerformanceDoesNotExist() throws Exception {
        mockMvc.perform(get("/performances/{id}/setlists", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    private PerformanceSetlist saveMySetlist() {
        return performanceSetlistRepository.save(PerformanceSetlist.create(
                performance,
                "Blue Spring",
                "Main Band",
                1,
                "Opening"
        ));
    }

    private PerformanceSetlist saveOtherPerformanceSetlist() {
        AdminUser otherAdminUser = adminUserRepository.save(createAdminUser("other-performer-admin"));
        Performance otherPerformance = performanceRepository.save(Performance.create(otherAdminUser, "Other Stage"));
        return performanceSetlistRepository.save(PerformanceSetlist.create(
                otherPerformance,
                "Other Song",
                "Other Band",
                1,
                null
        ));
    }

    private AdminUser createAdminUser(String loginId) {
        return AdminUser.create(
                loginId,
                "password-hash",
                "Performance Team",
                AdminRole.PERFORMER,
                "Representative",
                "010-0000-0000",
                null
        );
    }

    private void mockCurrentAdmin(AdminUser adminUser) {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(adminUser.getId(), AdminRole.PERFORMER, adminUser.getLoginId()));
    }
}
