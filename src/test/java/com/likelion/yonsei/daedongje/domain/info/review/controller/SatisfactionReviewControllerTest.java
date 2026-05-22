package com.likelion.yonsei.daedongje.domain.info.review.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.yonsei.daedongje.domain.info.review.repository.SatisfactionReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SatisfactionReviewControllerTest {

    private static final String REVIEWS_URL = "/api/info/reviews";
    private static final String INSTAGRAM_URL = "https://www.instagram.com/likelion_yonsei";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SatisfactionReviewRepository satisfactionReviewRepository;

    @BeforeEach
    void setUp() {
        satisfactionReviewRepository.deleteAll();
    }

    @Test
    void createReviewReturnsCreatedWhenRatingAndContentAreProvided() throws Exception {
        String requestBody = """
                {
                  "rating": 5,
                  "content": "The website was easy to use and the festival information was helpful."
                }
                """;

        mockMvc.perform(post(REVIEWS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content")
                        .value("The website was easy to use and the festival information was helpful."))
                .andExpect(jsonPath("$.data.instagramUrl").value(INSTAGRAM_URL))
                .andExpect(jsonPath("$.data.createdAt").exists());

        assertThat(satisfactionReviewRepository.count()).isEqualTo(1);
    }

    @Test
    void createReviewReturnsCreatedWhenOnlyRatingIsProvided() throws Exception {
        String requestBody = """
                {
                  "rating": 4
                }
                """;

        mockMvc.perform(post(REVIEWS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.content").doesNotExist())
                .andExpect(jsonPath("$.data.instagramUrl").value(INSTAGRAM_URL));

        assertThat(satisfactionReviewRepository.count()).isEqualTo(1);
    }

    @Test
    void createReviewReturnsBadRequestWhenRatingIsMissing() throws Exception {
        String requestBody = """
                {
                  "content": "No rating"
                }
                """;

        mockMvc.perform(post(REVIEWS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(satisfactionReviewRepository.count()).isZero();
    }

    @Test
    void createReviewReturnsBadRequestWhenRatingIsLessThanOne() throws Exception {
        String requestBody = """
                {
                  "rating": 0,
                  "content": "Invalid rating"
                }
                """;

        mockMvc.perform(post(REVIEWS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(satisfactionReviewRepository.count()).isZero();
    }

    @Test
    void createReviewReturnsBadRequestWhenRatingIsGreaterThanFive() throws Exception {
        String requestBody = """
                {
                  "rating": 6,
                  "content": "Invalid rating"
                }
                """;

        mockMvc.perform(post(REVIEWS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(satisfactionReviewRepository.count()).isZero();
    }

    @Test
    void createReviewReturnsBadRequestWhenContentIsLongerThanOneThousandCharacters() throws Exception {
        String requestBody = """
                {
                  "rating": 5,
                  "content": "%s"
                }
                """.formatted("a".repeat(1001));

        mockMvc.perform(post(REVIEWS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(satisfactionReviewRepository.count()).isZero();
    }

    @Test
    void openApiExposesSatisfactionReviewApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = apiDocs.path("paths");
        JsonNode requestSchema = paths.path(REVIEWS_URL)
                .path("post")
                .path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema");
        JsonNode responses = paths.path(REVIEWS_URL)
                .path("post")
                .path("responses");

        assertThat(apiDocs.path("tags").toString()).contains("만족도 리뷰");
        assertThat(paths.path(REVIEWS_URL).has("post")).isTrue();
        assertThat(requestSchema.path("$ref").asText()).contains("SatisfactionReviewCreateRequest");
        assertThat(responses.path("201").toString()).contains("ApiResponseSatisfactionReviewCreateResponse");
        assertThat(apiDocs.path("components").path("schemas").has("SatisfactionReviewCreateResponse")).isTrue();
    }
}
