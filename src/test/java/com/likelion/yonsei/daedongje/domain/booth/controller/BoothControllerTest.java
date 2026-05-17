package com.likelion.yonsei.daedongje.domain.booth.controller;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.festival.FestivalDayService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothClickLogService;
import com.likelion.yonsei.daedongje.domain.booth.service.BoothService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoothController.class)
class BoothControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoothService boothService;

    @MockitoBean
    private FestivalDayService festivalDayService;

    @MockitoBean
    private BoothClickLogService boothClickLogService;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("부스 클릭 로그 저장 API는 데이터 없는 성공 응답을 반환한다")
    void createClickLogReturnsSuccessEmpty() throws Exception {
        mockMvc.perform(post("/api/booths/{boothId}/clicks", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(boothClickLogService).create(1L);
    }

    @Test
    @DisplayName("존재하지 않는 부스 클릭 로그 저장 요청은 404를 반환한다")
    void createClickLogWithUnknownBoothReturnsNotFound() throws Exception {
        doThrow(new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND))
                .when(boothClickLogService).create(999L);

        mockMvc.perform(post("/api/booths/{boothId}/clicks", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("B-001"))
                .andExpect(jsonPath("$.error.message").value("존재하지 않는 부스입니다."));
    }
}
