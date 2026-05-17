package com.likelion.yonsei.daedongje.domain.map.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.exception.MapLocationErrorCode;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapLocationServiceTest {

    @Mock
    private MapLocationRepository mapLocationRepository;

    @InjectMocks
    private MapLocationService mapLocationService;

    @Test
    void deleteDeletesLocationAndFlushesImmediately() {
        MapLocation mapLocation = mapLocation();
        when(mapLocationRepository.findById(1L)).thenReturn(Optional.of(mapLocation));

        mapLocationService.delete(1L);

        verify(mapLocationRepository).delete(mapLocation);
        verify(mapLocationRepository).flush();
    }

    @Test
    void deleteConvertsDataIntegrityViolationToConflictBusinessException() {
        MapLocation mapLocation = mapLocation();
        when(mapLocationRepository.findById(1L)).thenReturn(Optional.of(mapLocation));
        doThrow(new DataIntegrityViolationException("referenced"))
                .when(mapLocationRepository)
                .flush();

        assertThatThrownBy(() -> mapLocationService.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MapLocationErrorCode.MAP_LOCATION_IN_USE));
    }

    private MapLocation mapLocation() {
        return MapLocation.create(
                "Main Stage",
                "A",
                new BigDecimal("123.4567"),
                new BigDecimal("45.6789"),
                null,
                null,
                MapLocationType.STAGE,
                0,
                MapDisplayStatus.VISIBLE
        );
    }
}
