package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothImageRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.MenuRepository;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
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
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class BoothServiceTest {

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private BoothImageRepository boothImageRepository;

    @Mock
    private BoothClickLogRepository boothClickLogRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MapLocationRepository mapLocationRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private NoticeRepository noticeRepository;

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

    @Test
    @DisplayName("자식 데이터가 없는 부스는 정상적으로 삭제된다")
    void deleteRemovesBoothWhenNoChildData() {
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(false);
        when(menuRepository.existsByBoothId(7L)).thenReturn(false);
        when(noticeRepository.existsByBoothId(7L)).thenReturn(false);

        boothService.delete(7L);

        verify(boothRepository).delete(booth);
    }

    @Test
    @DisplayName("예약이 있는 부스는 BOOTH_HAS_RESERVATIONS 로 삭제가 차단된다")
    void deleteBlocksWhenReservationsExist() {
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(true);

        assertThatThrownBy(() -> boothService.delete(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.BOOTH_HAS_RESERVATIONS));

        verify(boothRepository, never()).delete(any(Booth.class));
    }

    @Test
    @DisplayName("메뉴가 있는 부스는 BOOTH_HAS_MENUS 로 삭제가 차단된다")
    void deleteBlocksWhenMenusExist() {
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(false);
        when(menuRepository.existsByBoothId(7L)).thenReturn(true);

        assertThatThrownBy(() -> boothService.delete(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.BOOTH_HAS_MENUS));

        verify(boothRepository, never()).delete(any(Booth.class));
    }

    @Test
    @DisplayName("공지가 있는 부스는 BOOTH_HAS_NOTICES 로 삭제가 차단된다")
    void deleteBlocksWhenNoticesExist() {
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(false);
        when(menuRepository.existsByBoothId(7L)).thenReturn(false);
        when(noticeRepository.existsByBoothId(7L)).thenReturn(true);

        assertThatThrownBy(() -> boothService.delete(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.BOOTH_HAS_NOTICES));

        verify(boothRepository, never()).delete(any(Booth.class));
    }

    @Test
    @DisplayName("부스 삭제 시 자식 가드 통과 후 클릭로그·이미지가 application-level 에서 정리된 뒤 부스가 삭제된다")
    void deleteCascadesClickLogsAndImagesBeforeRemovingBooth() {
        // BAC-111 — 운영 DB 의 FK 가 ON DELETE CASCADE 가 아닌 경우에도 정리되도록
        // BoothClickLog·BoothImage 를 application-level 에서 명시적으로 삭제한다.
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(false);
        when(menuRepository.existsByBoothId(7L)).thenReturn(false);
        when(noticeRepository.existsByBoothId(7L)).thenReturn(false);

        boothService.delete(7L);

        // 순서 보장 — 자식 정리는 부스 삭제 *전에* 일어나야 한다. FK 가 RESTRICT 인
        // 환경에서 순서가 뒤집히면 boothRepository.delete 가 즉시 FK 위반으로 실패한다.
        InOrder inOrder = inOrder(boothClickLogRepository, boothImageRepository, boothRepository);
        inOrder.verify(boothClickLogRepository).deleteByBoothId(7L);
        inOrder.verify(boothImageRepository).deleteByBoothId(7L);
        inOrder.verify(boothRepository).delete(booth);
    }

    @Test
    @DisplayName("자식 가드(예약·메뉴·공지) 에 걸려 삭제가 차단되면 클릭로그·이미지도 정리되지 않는다")
    void deleteSkipsCascadeWhenGuardBlocks() {
        // 자식 가드 통과 *후* cascade — 가드에서 막혔다면 cascade 도 건너뛰어야 한다.
        // 그렇지 않으면 "삭제 실패" 응답인데 이미지/로그만 사라지는 일관성 깨짐이 발생한다.
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(true);

        assertThatThrownBy(() -> boothService.delete(7L))
                .isInstanceOf(BusinessException.class);

        verify(boothClickLogRepository, never()).deleteByBoothId(any());
        verify(boothImageRepository, never()).deleteByBoothId(any());
    }

    @Test
    @DisplayName("검사 통과 후 race 로 예약이 생겨 FK 위반이 나면 BusinessException 으로 변환된다")
    void deleteConvertsFkViolationToBusinessExceptionOnRace() {
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        // 1차 검사(verifyNoChildData) — 자식 없음. 2차 recheck — 예약이 race 로 생김.
        when(reservationRepository.existsByBoothId(7L)).thenReturn(false, true);
        when(menuRepository.existsByBoothId(7L)).thenReturn(false);
        when(noticeRepository.existsByBoothId(7L)).thenReturn(false);
        // 1차 통과 후 boothRepository.delete 가 race 로 FK 위반 (또는 flush 시점에 위반)
        doThrow(new DataIntegrityViolationException("FK violation: reservations.fk_reservations_booth"))
                .when(boothRepository).delete(booth);

        assertThatThrownBy(() -> boothService.delete(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.BOOTH_HAS_RESERVATIONS));
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
