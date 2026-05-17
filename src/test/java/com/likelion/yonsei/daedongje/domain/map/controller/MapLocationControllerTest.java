package com.likelion.yonsei.daedongje.domain.map.controller;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminAuthContextService;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MapLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MapLocationRepository mapLocationRepository;

    @MockitoBean
    private AdminAuthContextService adminAuthContextService;

    @BeforeEach
    void setUp() {
        mapLocationRepository.deleteAll();
        Mockito.reset(adminAuthContextService);
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(1L, AdminRole.MASTER, "map-admin"));
    }

    @Test
    void updateMapLocationUpdatesOnlyProvidedFields() throws Exception {
        MapLocation mapLocation = mapLocationRepository.save(MapLocation.create(
                "Main Stage",
                "A",
                new BigDecimal("123.4567"),
                new BigDecimal("45.6789"),
                new BigDecimal("12.345"),
                new BigDecimal("6.789"),
                MapLocationType.STAGE,
                1,
                MapDisplayStatus.VISIBLE
        ));

        String requestBody = """
                {
                  "locationName": "Updated Stage",
                  "mapX": 234.5678,
                  "displayStatus": "HIDDEN"
                }
                """;

        mockMvc.perform(patch("/api/admin/map-locations/{id}", mapLocation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locationName").value("Updated Stage"))
                .andExpect(jsonPath("$.data.sector").value("A"))
                .andExpect(jsonPath("$.data.mapX").value(234.5678))
                .andExpect(jsonPath("$.data.mapY").value(45.6789))
                .andExpect(jsonPath("$.data.locationType").value("STAGE"))
                .andExpect(jsonPath("$.data.displayOrder").value(1))
                .andExpect(jsonPath("$.data.displayStatus").value("HIDDEN"));
    }

    @Test
    void updateMapLocationRejectsBlankStringFields() throws Exception {
        MapLocation mapLocation = mapLocationRepository.save(MapLocation.create(
                "Main Stage",
                "A",
                new BigDecimal("123.4567"),
                new BigDecimal("45.6789"),
                null,
                null,
                MapLocationType.STAGE,
                0,
                MapDisplayStatus.VISIBLE
        ));

        String requestBody = """
                {
                  "locationName": "   "
                }
                """;

        mockMvc.perform(patch("/api/admin/map-locations/{id}", mapLocation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMapLocationRejectsBoothRole() throws Exception {
        Mockito.when(adminAuthContextService.getCurrentAdmin(any(HttpServletRequest.class)))
                .thenReturn(new AdminSessionUser(2L, AdminRole.BOOTH, "booth-admin"));

        String requestBody = """
                {
                  "locationName": "Updated Stage"
                }
                """;

        mockMvc.perform(patch("/api/admin/map-locations/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }
}
