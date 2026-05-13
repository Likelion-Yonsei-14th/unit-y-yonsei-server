package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.domain.info.entity.BarrierFreeInfo;
import com.likelion.yonsei.daedongje.domain.info.repository.BarrierFreeInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BarrierFreeInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BarrierFreeInfoRepository barrierFreeInfoRepository;

    @BeforeEach
    void setUp() {
        barrierFreeInfoRepository.deleteAll();
    }

    @Test
    void getBarrierFreeInfos_returnsOrderedList() throws Exception {
        barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "휠체어 이동 동선",
                "정문에서 본부석까지 이동 가능한 동선입니다.",
                "https://example.com/map-2.png",
                "ROUTE",
                2,
                20L
        ));
        barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "장애인 화장실 안내",
                "학생회관 1층 화장실을 이용해주세요.",
                "https://example.com/map-1.png",
                "TOILET",
                1,
                10L
        ));

        mockMvc.perform(get("/api/barrier-free-infos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("장애인 화장실 안내"))
                .andExpect(jsonPath("$.data[0].facility_type").value("TOILET"))
                .andExpect(jsonPath("$.data[0].map_location_id").value(10))
                .andExpect(jsonPath("$.data[1].title").value("휠체어 이동 동선"));
    }

    @Test
    void getBarrierFreeInfos_filtersByFacilityType() throws Exception {
        barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "장애인 화장실 안내",
                "학생회관 1층 화장실을 이용해주세요.",
                null,
                "TOILET",
                1,
                10L
        ));
        barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "휠체어 이동 동선",
                "정문에서 본부석까지 이동 가능한 동선입니다.",
                null,
                "ROUTE",
                2,
                20L
        ));

        mockMvc.perform(get("/api/barrier-free-infos")
                        .param("facility_type", "TOILET")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].facility_type").value("TOILET"));
    }

    @Test
    void getBarrierFreeInfo_returnsSingleItem() throws Exception {
        BarrierFreeInfo barrierFreeInfo = barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "배리어프리 안내 맵",
                "무장애 경로를 확인할 수 있습니다.",
                "https://example.com/map.png",
                "MAP",
                1,
                5L
        ));

        mockMvc.perform(get("/api/barrier-free-infos/{id}", barrierFreeInfo.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(barrierFreeInfo.getId()))
                .andExpect(jsonPath("$.data.guide_map_image_url").value("https://example.com/map.png"));
    }
}
