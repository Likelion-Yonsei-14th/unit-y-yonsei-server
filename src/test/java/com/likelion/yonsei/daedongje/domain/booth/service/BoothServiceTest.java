package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothImageRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoothServiceTest {

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private BoothImageRepository boothImageRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MapLocationRepository mapLocationRepository;

    @InjectMocks
    private BoothService boothService;

    @Test
    @DisplayName("지도 위치가 지정되지 않은 부스만 있어도 목록 조회는 NPE 없이 동작하고 mapLocation 은 null 이다")
    void getListReturnsNullMapLocationWhenNoBoothHasLocationId() {
        // locationId 가 모두 null 이면 fetchMapLocationMap 이 빈 불변 맵(Map.of())을 반환한다.
        // 수정 전에는 mapLocationMap.get(null) 호출이 NPE 를 던져 목록 API 전체가 500 이 됐다.
        when(boothRepository.findAll()).thenReturn(List.of(booth(1L, null)));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(1L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of());
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(1L)))
                .thenReturn(List.of());

        List<BoothResponse> responses = boothService.getList(null, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getMapLocation()).isNull();
    }

    @Test
    @DisplayName("locationId 가 지정된 부스는 목록 응답에 지도 위치 정보가 채워진다")
    void getListPopulatesMapLocationWhenBoothHasLocationId() {
        when(boothRepository.findAll()).thenReturn(List.of(booth(1L, 10L)));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(1L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of());
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(1L)))
                .thenReturn(List.of());
        when(mapLocationRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(mapLocation(10L)));

        List<BoothResponse> responses = boothService.getList(null, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getMapLocation()).isNotNull();
        assertThat(responses.get(0).getMapLocation().getId()).isEqualTo(10L);
    }

    private Booth booth(Long id, Long locationId) {
        Booth booth = Booth.create(
                1L,
                "멋사 핫도그",
                "멋쟁이사자처럼",
                "소개",
                2,
                LocalTime.of(11, 0),
                LocalTime.of(20, 0),
                BoothSector.한글탑,
                3,
                BoothStatus.OPEN,
                true,
                null,
                true,
                null,
                locationId,
                null,
                false,
                null
        );
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private MapLocation mapLocation(Long id) {
        MapLocation location = MapLocation.create(
                "한글탑 A-3",
                "한글탑",
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(1.0),
                MapLocationType.BOOTH,
                1,
                MapDisplayStatus.VISIBLE
        );
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }
}
