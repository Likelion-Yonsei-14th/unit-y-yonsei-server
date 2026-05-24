package com.likelion.yonsei.daedongje.domain.image.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ImageUploadControllerTest {

    private static final String EXPECTED_CACHE_CONTROL = "public, max-age=31536000, immutable";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    // 실제 S3Presigner 빈을 대체해 AWS 자격증명 없이 테스트가 돌도록 한다.
    @MockitoBean
    private S3Presigner s3Presigner;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(adminAuthContextService, s3Presigner);

        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(1L, AdminRole.SUPER, "super-admin"));

        PresignedPutObjectRequest presigned = Mockito.mock(PresignedPutObjectRequest.class);
        Mockito.when(presigned.url())
                .thenReturn(URI.create(
                        "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/booth/uuid.webp?X-Amz-Signature=stub"
                ).toURL());
        Mockito.when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presigned);
    }

    @Test
    void createPresignedUrlSignsAndReturnsSameCacheControl() throws Exception {
        String requestBody = """
                {
                  "domain": "booth",
                  "fileName": "thumbnail.webp",
                  "contentType": "image/webp",
                  "fileSize": 12345
                }
                """;

        mockMvc.perform(post("/api/admin/images/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadUrl").value(startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/")))
                .andExpect(jsonPath("$.data.objectKey").value(startsWith("images/booth/")))
                .andExpect(jsonPath("$.data.objectKey").value(endsWith(".webp")))
                .andExpect(jsonPath("$.data.imageUrl").value(startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/booth/")))
                // FE가 PUT 헤더로 echo 해야 하는 값이 응답에 그대로 실린다.
                .andExpect(jsonPath("$.data.cacheControl").value(EXPECTED_CACHE_CONTROL));

        // 서명에 박힌 값(PutObjectRequest.cacheControl)이 응답으로 내려준 값과 동일해야
        // 클라이언트 echo 시 SignatureMatch가 성립한다. 이 일치가 이 기능의 핵심 불변식.
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        Mockito.verify(s3Presigner).presignPutObject(captor.capture());

        PutObjectRequest signedPut = captor.getValue().putObjectRequest();
        assertThat(signedPut.cacheControl()).isEqualTo(EXPECTED_CACHE_CONTROL);
        assertThat(signedPut.contentType()).isEqualTo("image/webp");
        assertThat(signedPut.contentLength()).isEqualTo(12345L);
    }
}
