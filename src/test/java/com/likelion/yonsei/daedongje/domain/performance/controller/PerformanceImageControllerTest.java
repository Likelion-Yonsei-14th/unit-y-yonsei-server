package com.likelion.yonsei.daedongje.domain.performance.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImageType;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceImageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
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
class PerformanceImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private PerformanceImageRepository performanceImageRepository;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    private AdminUser adminUser;
    private Performance performance;

    @BeforeEach
    void setUp() {
        performanceImageRepository.deleteAll();
        performanceRepository.deleteAll();
        adminUserRepository.deleteAll();
        Mockito.reset(adminAuthContextService);

        adminUser = adminUserRepository.save(createAdminUser("performer-admin"));
        performance = performanceRepository.save(Performance.create(adminUser, "Main Stage"));
        // 사용자 공개 조회 테스트를 위해 공연을 노출 상태로 전환한다 (생성 시 기본값은 HIDDEN)
        performance.updateBasicInfo(null, null, null, null, null, null, null, null, PerformanceStatus.SCHEDULED);
        performance = performanceRepository.save(performance);

        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(adminUser.getId(), AdminRole.PERFORMER, adminUser.getLoginId()));
    }

    @Test
    void createMyPerformanceImageSavesImageForCurrentAdminPerformance() throws Exception {
        String requestBody = """
                {
                  "imageUrl": "https://example-bucket.s3.ap-northeast-2.amazonaws.com/performance/example.png",
                  "imageOrder": 1,
                  "imageType": "PROFILE"
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.performanceId").value(performance.getId()))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example-bucket.s3.ap-northeast-2.amazonaws.com/performance/example.png"))
                .andExpect(jsonPath("$.data.imageOrder").value(1))
                .andExpect(jsonPath("$.data.imageType").value("PROFILE"));
    }

    @Test
    void createMyPerformanceImageReturnsBadRequestWhenImageUrlIsBlank() throws Exception {
        String requestBody = """
                {
                  "imageUrl": "   ",
                  "imageOrder": 1,
                  "imageType": "PROFILE"
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceImageReturnsBadRequestWhenImageOrderIsNull() throws Exception {
        String requestBody = """
                {
                  "imageUrl": "https://example.com/profile.png",
                  "imageOrder": null,
                  "imageType": "PROFILE"
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceImageReturnsBadRequestWhenImageOrderIsLessThanOne() throws Exception {
        String requestBody = """
                {
                  "imageUrl": "https://example.com/profile.png",
                  "imageOrder": 0,
                  "imageType": "PROFILE"
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceImageReturnsBadRequestWhenImageTypeIsNull() throws Exception {
        String requestBody = """
                {
                  "imageUrl": "https://example.com/profile.png",
                  "imageOrder": 1,
                  "imageType": null
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createMyPerformanceImageReturnsNotFoundWhenCurrentAdminHasNoPerformance() throws Exception {
        AdminUser adminWithoutPerformance = adminUserRepository.save(createAdminUser("admin-without-performance"));
        mockCurrentAdmin(adminWithoutPerformance);

        String requestBody = """
                {
                  "imageUrl": "https://example.com/profile.png",
                  "imageOrder": 1,
                  "imageType": "PROFILE"
                }
                """;

        mockMvc.perform(post("/api/admin/performances/me/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getPerformanceImagesReturnsOrderedImages() throws Exception {
        performanceImageRepository.save(PerformanceImage.create(
                performance,
                "https://example.com/second.png",
                2,
                PerformanceImageType.DETAIL
        ));
        PerformanceImage first = performanceImageRepository.save(PerformanceImage.create(
                performance,
                "https://example.com/first.png",
                1,
                PerformanceImageType.PROFILE
        ));
        PerformanceImage sameOrderSecond = performanceImageRepository.save(PerformanceImage.create(
                performance,
                "https://example.com/same-order-second.png",
                1,
                PerformanceImageType.DETAIL
        ));

        mockMvc.perform(get("/api/performances/{id}/images", performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").value(first.getId()))
                .andExpect(jsonPath("$.data[1].id").value(sameOrderSecond.getId()))
                .andExpect(jsonPath("$.data[2].imageOrder").value(2));
    }

    @Test
    void getPerformanceImagesReturnsEmptyArrayWhenPerformanceHasNoImages() throws Exception {
        mockMvc.perform(get("/api/performances/{id}/images", performance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getPerformanceImagesReturnsNotFoundWhenPerformanceDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/performances/{id}/images", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void getPerformanceImagesReturnsNotFoundWhenPerformanceIsHidden() throws Exception {
        AdminUser hiddenAdmin = adminUserRepository.save(createAdminUser("hidden-performer-admin"));
        Performance hiddenPerformance = performanceRepository.save(Performance.create(hiddenAdmin, "Hidden Stage"));
        // Performance.create의 기본 상태는 HIDDEN이므로 별도 전환 없이 비공개 상태다
        performanceImageRepository.save(PerformanceImage.create(
                hiddenPerformance,
                "https://example.com/hidden.png",
                1,
                PerformanceImageType.PROFILE
        ));

        mockMvc.perform(get("/api/performances/{id}/images", hiddenPerformance.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void deleteMyPerformanceImageDeletesOwnImage() throws Exception {
        PerformanceImage image = performanceImageRepository.save(PerformanceImage.create(
                performance,
                "https://example.com/profile.png",
                1,
                PerformanceImageType.PROFILE
        ));

        mockMvc.perform(delete("/api/admin/performances/me/images/{imageId}", image.getId()))
                .andExpect(status().isNoContent());

        assertThat(performanceImageRepository.findById(image.getId())).isEmpty();
    }

    @Test
    void deleteMyPerformanceImageReturnsNotFoundWhenImageDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/admin/performances/me/images/{imageId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-008"));
    }

    @Test
    void deleteMyPerformanceImageReturnsNotFoundWhenCurrentAdminHasNoPerformance() throws Exception {
        AdminUser otherAdminUser = adminUserRepository.save(createAdminUser("other-performer-admin"));
        Performance otherPerformance = performanceRepository.save(Performance.create(otherAdminUser, "Other Stage"));
        PerformanceImage otherImage = performanceImageRepository.save(PerformanceImage.create(
                otherPerformance,
                "https://example.com/other.png",
                1,
                PerformanceImageType.PROFILE
        ));
        AdminUser adminWithoutPerformance = adminUserRepository.save(createAdminUser("admin-without-performance"));
        mockCurrentAdmin(adminWithoutPerformance);

        mockMvc.perform(delete("/api/admin/performances/me/images/{imageId}", otherImage.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }

    @Test
    void deleteMyPerformanceImageReturnsForbiddenWhenImageBelongsToAnotherPerformance() throws Exception {
        AdminUser otherAdminUser = adminUserRepository.save(createAdminUser("other-performer-admin"));
        Performance otherPerformance = performanceRepository.save(Performance.create(otherAdminUser, "Other Stage"));
        PerformanceImage otherImage = performanceImageRepository.save(PerformanceImage.create(
                otherPerformance,
                "https://example.com/other.png",
                1,
                PerformanceImageType.PROFILE
        ));

        mockMvc.perform(delete("/api/admin/performances/me/images/{imageId}", otherImage.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A-009"));
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
