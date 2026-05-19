package com.likelion.yonsei.daedongje.domain.home.controller;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.home.service.HomeService;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCurrentResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
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
    @DisplayName("현재 진행 중인 공연 조회 API는 공연 정보를 공통 성공 응답으로 반환한다")
    void getCurrentPerformanceReturnsPerformance() throws Exception {
        when(homeService.getCurrentPerformance()).thenReturn(
                PerformanceCurrentResponse.builder()
                        .id(1L)
                        .performanceName("연세 밴드부 YB")
                        .startTime(java.time.LocalTime.of(18, 0))
                        .endTime(java.time.LocalTime.of(18, 40))
                        .performanceStatus(PerformanceStatus.ONGOING)
                        .performanceCategory(PerformanceCategory.ARTIST)
                        .locationId(10L)
                        .locationName("노천극장")
                        .build()
        );

        mockMvc.perform(get("/api/home/current-performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.performanceName").value("연세 밴드부 YB"))
                .andExpect(jsonPath("$.data.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("18:40:00"))
                .andExpect(jsonPath("$.data.performanceStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.performanceCategory").value("ARTIST"))
                .andExpect(jsonPath("$.data.locationId").value(10))
                .andExpect(jsonPath("$.data.locationName").value("노천극장"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("현재 진행 중인 공연이 없으면 기존 공연 도메인 예외 응답을 반환한다")
    void getCurrentPerformanceReturnsNotFoundWhenNoCurrentPerformance() throws Exception {
        doThrow(new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND))
                .when(homeService).getCurrentPerformance();

        mockMvc.perform(get("/api/home/current-performance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P-006"));
    }
}
