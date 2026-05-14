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
                "Route info",
                "Accessible route guidance",
                "https://example.com/map-2.png",
                "ROUTE",
                2,
                20L
        ));
        barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "Toilet info",
                "Accessible toilet guidance",
                "https://example.com/map-1.png",
                "TOILET",
                1,
                10L
        ));

        mockMvc.perform(get("/api/barrier-free-infos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Toilet info"))
                .andExpect(jsonPath("$.data[0].facilityType").value("TOILET"))
                .andExpect(jsonPath("$.data[0].mapLocationId").value(10))
                .andExpect(jsonPath("$.data[1].title").value("Route info"));
    }

    @Test
    void getBarrierFreeInfos_filtersByFacilityType() throws Exception {
        barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "Toilet info",
                "Accessible toilet guidance",
                null,
                "TOILET",
                1,
                10L
        ));
        barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "Route info",
                "Accessible route guidance",
                null,
                "ROUTE",
                2,
                20L
        ));

        mockMvc.perform(get("/api/barrier-free-infos")
                        .param("facilityType", "TOILET")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].facilityType").value("TOILET"));
    }

    @Test
    void getBarrierFreeInfo_returnsSingleItem() throws Exception {
        BarrierFreeInfo barrierFreeInfo = barrierFreeInfoRepository.save(BarrierFreeInfo.create(
                "Guide map",
                "Barrier-free map guidance",
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
                .andExpect(jsonPath("$.data.guideMapImageUrl").value("https://example.com/map.png"));
    }
}
