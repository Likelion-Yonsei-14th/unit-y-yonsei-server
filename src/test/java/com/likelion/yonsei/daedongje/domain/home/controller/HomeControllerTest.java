package com.likelion.yonsei.daedongje.domain.home.controller;

import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.home.dto.HomePopularBoothResponse;
import com.likelion.yonsei.daedongje.domain.home.service.HomeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeService homeService;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("메인 배너 목록 조회 API는 빈 목록도 공통 성공 응답으로 반환한다")
    void getBannersReturnsEmptyList() throws Exception {
        when(homeService.getBanners()).thenReturn(List.of());

        mockMvc.perform(get("/api/home/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("오늘의 인기 부스 조회 API는 부스 정보와 집계 정보를 반환한다")
    void getPopularBoothsReturnsRankedList() throws Exception {
        when(homeService.getPopularBooths()).thenReturn(List.of(
                HomePopularBoothResponse.builder()
                        .rank(1)
                        .boothId(12L)
                        .clickCount(37L)
                        .name("호프 한 잔")
                        .organization("사회학과")
                        .sector(BoothSector.백양로)
                        .location(7)
                        .status(BoothStatus.OPEN)
                        .isFood(true)
                        .isReservable(true)
                        .representativeMenus(List.of("치킨", "맥주"))
                        .waitingCount(2L)
                        .thumbnailUrl("https://example.com/thumbnail.jpg")
                        .build()
        ));

        mockMvc.perform(get("/api/home/popular-booths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].boothId").value(12))
                .andExpect(jsonPath("$.data[0].clickCount").value(37))
                .andExpect(jsonPath("$.data[0].name").value("호프 한 잔"))
                .andExpect(jsonPath("$.data[0].organization").value("사회학과"))
                .andExpect(jsonPath("$.data[0].sector").value("백양로"))
                .andExpect(jsonPath("$.data[0].location").value(7))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[0].isFood").value(true))
                .andExpect(jsonPath("$.data[0].isReservable").value(true))
                .andExpect(jsonPath("$.data[0].representativeMenus[0]").value("치킨"))
                .andExpect(jsonPath("$.data[0].representativeMenus[1]").value("맥주"))
                .andExpect(jsonPath("$.data[0].waitingCount").value(2))
                .andExpect(jsonPath("$.data[0].thumbnailUrl").value("https://example.com/thumbnail.jpg"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
