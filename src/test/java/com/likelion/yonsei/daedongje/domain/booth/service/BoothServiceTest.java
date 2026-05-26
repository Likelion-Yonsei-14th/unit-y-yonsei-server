package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.ReservableBoothResponse;
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

import com.likelion.yonsei.daedongje.common.exception.CommonErrorCode;
import com.likelion.yonsei.daedongje.common.response.PageResponse;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private AdminUserRepository adminUserRepository;

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

        PageResponse<BoothResponse> responses = boothService.getList(null, null, null, null, null, 0, 100);

        assertThat(responses.content()).hasSize(1);
        assertThat(responses.content().get(0).getMapLocation()).isNull();
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

        PageResponse<BoothResponse> responses = boothService.getList(null, null, null, null, null, 0, 100);

        assertThat(responses.content()).hasSize(1);
        assertThat(responses.content().get(0).getMapLocation()).isNotNull();
        assertThat(responses.content().get(0).getMapLocation().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("부스 목록은 page/size 로 페이지네이션된다")
    void getListPaginatesResults() {
        when(boothRepository.findAll()).thenReturn(List.of(booth(1L, null), booth(2L, null), booth(3L, null)));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(1L, 2L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of());
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(1L, 2L)))
                .thenReturn(List.of());

        PageResponse<BoothResponse> responses = boothService.getList(null, null, null, null, null, 0, 2);

        assertThat(responses.content()).hasSize(2);
        assertThat(responses.totalElements()).isEqualTo(3L);
        assertThat(responses.hasNext()).isTrue();
    }

    @Test
    @DisplayName("검색은 PageResponse 로 페이지네이션되고 waitingCount 를 집계한다")
    void searchReturnsPagedResultsWithWaitingCount() {
        when(boothRepository.searchByKeyword("멋사")).thenReturn(List.of(booth(1L, null)));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(1L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 4L}));
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(1L)))
                .thenReturn(List.of());

        PageResponse<BoothResponse> responses = boothService.search("멋사", 0, 20);

        assertThat(responses.content()).hasSize(1);
        assertThat(responses.content().get(0).getWaitingCount()).isEqualTo(4L);
        assertThat(responses.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("size 가 상한(100)을 초과하면 INVALID_INPUT 으로 거절한다")
    void getListRejectsSizeOverMax() {
        assertThatThrownBy(() -> boothService.getList(null, null, null, null, null, 0, 101))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("status 필터 지정 시 해당 운영상태 부스만 반환한다")
    void getListFiltersByStatus() {
        Booth open = booth(1L, null);
        Booth closed = booth(2L, null);
        ReflectionTestUtils.setField(closed, "status", BoothStatus.CLOSED);
        when(boothRepository.findAll()).thenReturn(List.of(open, closed));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(1L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of());
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(1L)))
                .thenReturn(List.of());

        PageResponse<BoothResponse> responses = boothService.getList(null, null, null, null, BoothStatus.OPEN, 0, 100);

        assertThat(responses.content()).hasSize(1);
    }

    @Test
    @DisplayName("예약 가능 목록은 OPEN·PREPARING 상태를 모두 조회한다 — 축제 전 PREPARING 부스 누락 방지 (R-01)")
    void getReservableListIncludesPreparingBooths() {
        Booth preparing = booth(1L, null);
        ReflectionTestUtils.setField(preparing, "status", BoothStatus.PREPARING);
        when(boothRepository.findAllByIsReservableAndStatusIn(eq(true), anyCollection()))
                .thenReturn(List.of(preparing));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(1L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(1L))).thenReturn(List.of());

        List<ReservableBoothResponse> result = boothService.getReservableList();

        // status=OPEN 단독 필터로 회귀하면(= PREPARING 누락 버그 재발) 이 검증이 실패한다.
        verify(boothRepository).findAllByIsReservableAndStatusIn(
                eq(true),
                argThat(statuses -> statuses.contains(BoothStatus.OPEN) && statuses.contains(BoothStatus.PREPARING)));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BoothStatus.PREPARING);
        assertThat(result.get(0).getWaitingCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("단건 조회 waitingCount 는 0 하드코딩이 아니라 실제 PENDING 예약 수를 반영한다 (B-01)")
    void getByIdReturnsActualWaitingCount() {
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth(7L, null)));
        when(reservationRepository.countByBoothIdAndStatus(7L, ReservationStatus.PENDING)).thenReturn(3L);
        when(boothImageRepository.findByBoothIdAndDisplayOrder(7L, 1)).thenReturn(Optional.empty());

        BoothResponse response = boothService.getById(7L);

        assertThat(response.getWaitingCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("검색 waitingCount 는 0 하드코딩이 아니라 부스별 실제 PENDING 예약 수를 반영한다 (B-01)")
    void searchReturnsActualWaitingCount() {
        when(boothRepository.searchByKeyword("핫도그")).thenReturn(List.of(booth(7L, null)));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(7L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of(new Object[]{7L, 2L}));
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(7L))).thenReturn(List.of());

        PageResponse<BoothResponse> responses = boothService.search("핫도그", 0, 20);

        assertThat(responses.content()).hasSize(1);
        assertThat(responses.content().get(0).getWaitingCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("상태 변경 응답 waitingCount 도 실제 PENDING 예약 수를 반영한다 (B-01)")
    void updateStatusReturnsActualWaitingCount() {
        AdminSessionUser superAdmin = new AdminSessionUser(1L, AdminRole.SUPER, "super");
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth(7L, null)));
        when(reservationRepository.countByBoothIdAndStatus(7L, ReservationStatus.PENDING)).thenReturn(5L);
        when(boothImageRepository.findByBoothIdAndDisplayOrder(7L, 1)).thenReturn(Optional.empty());

        BoothResponse response = boothService.updateStatus(7L, BoothStatus.OPEN, superAdmin);

        assertThat(response.getWaitingCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("자식 데이터가 없는 부스는 정상적으로 삭제된다")
    void deleteRemovesBoothWhenNoChildData() {
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(false);
        when(menuRepository.existsByBoothId(7L)).thenReturn(false);
        when(noticeRepository.existsByBoothId(7L)).thenReturn(false);

        boothService.delete(7L, superAdmin());

        verify(boothRepository).delete(booth);
    }

    @Test
    @DisplayName("예약이 있는 부스는 BOOTH_HAS_RESERVATIONS 로 삭제가 차단된다")
    void deleteBlocksWhenReservationsExist() {
        Booth booth = booth(7L, null);
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(true);

        assertThatThrownBy(() -> boothService.delete(7L, superAdmin()))
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

        assertThatThrownBy(() -> boothService.delete(7L, superAdmin()))
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

        assertThatThrownBy(() -> boothService.delete(7L, superAdmin()))
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

        boothService.delete(7L, superAdmin());

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

        assertThatThrownBy(() -> boothService.delete(7L, superAdmin()))
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

        assertThatThrownBy(() -> boothService.delete(7L, superAdmin()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.BOOTH_HAS_RESERVATIONS));
    }

    @Test
    @DisplayName("BOOTH 역할이 본인 담당이 아닌 부스를 삭제하려 하면 FORBIDDEN 으로 차단된다")
    void deleteBlockedForNonOwnerBoothAdmin() {
        Booth booth = booth(7L, null); // adminId = 1L
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));

        assertThatThrownBy(() -> boothService.delete(7L, boothAdmin(2L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.FORBIDDEN));

        verify(boothRepository, never()).delete(any(Booth.class));
    }

    @Test
    @DisplayName("BOOTH 역할이 본인 담당 부스는 삭제할 수 있다")
    void deleteAllowedForOwnerBoothAdmin() {
        Booth booth = booth(7L, null); // adminId = 1L
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(reservationRepository.existsByBoothId(7L)).thenReturn(false);
        when(menuRepository.existsByBoothId(7L)).thenReturn(false);
        when(noticeRepository.existsByBoothId(7L)).thenReturn(false);

        boothService.delete(7L, boothAdmin(1L));

        verify(boothRepository).delete(booth);
    }

    @Test
    @DisplayName("BOOTH 역할이 본인 담당이 아닌 부스를 수정하려 하면 FORBIDDEN 으로 차단된다")
    void updateBlockedForNonOwnerBoothAdmin() {
        Booth booth = booth(7L, null); // adminId = 1L
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));

        assertThatThrownBy(() -> boothService.update(7L, updateRequest(), boothAdmin(2L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("BOOTH 역할이 본인 담당 부스는 수정할 수 있다")
    void updateAllowedForOwnerBoothAdmin() {
        Booth booth = booth(7L, null); // adminId = 1L, name "멋사 핫도그"
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(boothImageRepository.findByBoothIdAndDisplayOrder(7L, 1)).thenReturn(Optional.empty());

        BoothResponse response = boothService.update(7L, updateRequest(), boothAdmin(1L));

        assertThat(response.getName()).isEqualTo("멋사 핫도그");
    }

    @Test
    @DisplayName("수정 중 name UNIQUE 동시성 충돌(flush 시 DataIntegrityViolation)은 DUPLICATE_BOOTH_NAME 으로 매핑된다")
    void updateMapsConstraintViolationOnFlushToDuplicateBoothName() {
        // update() 는 dirty checking 에만 의존하면 UPDATE 가 커밋 시점(try/catch 밖)에 나가 catch 를 빠져나가
        // 500 이 된다. 명시적 flush 로 try 안에서 UPDATE 가 실행돼야 이 catch 가 잡는다.
        Booth booth = booth(7L, null); // adminId 1L, name "멋사 핫도그" — 이름 동일이라 선검증 단락
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        doThrow(new DataIntegrityViolationException("unique constraint uq_booths_name"))
                .when(boothRepository).flush();

        assertThatThrownBy(() -> boothService.update(7L, updateRequest(), boothAdmin(1L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.DUPLICATE_BOOTH_NAME));
    }

    @Test
    @DisplayName("존재하지 않는 adminId 로 부스를 생성하면 ADMIN_USER_NOT_FOUND 로 차단되고 저장되지 않는다")
    void createBlockedWhenAdminDoesNotExist() {
        when(adminUserRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> boothService.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.ADMIN_USER_NOT_FOUND));

        verify(boothRepository, never()).save(any(Booth.class));
    }

    @Test
    @DisplayName("이미 부스가 배정된 adminId 로 부스를 생성하면 DUPLICATE_BOOTH_ADMIN 으로 차단되고 저장되지 않는다")
    void createBlockedWhenAdminAlreadyHasBooth() {
        when(adminUserRepository.existsById(1L)).thenReturn(true);
        when(boothRepository.existsByAdminId(1L)).thenReturn(true);

        assertThatThrownBy(() -> boothService.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.DUPLICATE_BOOTH_ADMIN));

        verify(boothRepository, never()).save(any(Booth.class));
    }

    @Test
    @DisplayName("존재하고 아직 담당 부스가 없는 adminId 로는 부스가 정상 생성된다")
    void createSucceedsWhenAdminExistsAndUnassigned() {
        when(adminUserRepository.existsById(1L)).thenReturn(true);
        when(boothRepository.existsByAdminId(1L)).thenReturn(false);
        when(boothRepository.existsByNameAndSector("멋사 핫도그", BoothSector.한글탑)).thenReturn(false);
        when(boothRepository.save(any(Booth.class))).thenAnswer(invocation -> {
            Booth saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        BoothResponse response = boothService.create(createRequest());

        assertThat(response.getName()).isEqualTo("멋사 핫도그");
    }

    @Test
    @DisplayName("선검증과 INSERT 사이 race 로 admin_id UNIQUE 제약에 걸리면 DUPLICATE_BOOTH_ADMIN 으로 매핑된다")
    void createMapsAdminIdUniqueViolationToDuplicateBoothAdmin() {
        when(adminUserRepository.existsById(1L)).thenReturn(true);
        // 선검증 시점엔 없음(false) → 동시 트랜잭션이 먼저 커밋 → 재확인 시 존재(true)
        when(boothRepository.existsByAdminId(1L)).thenReturn(false, true);
        when(boothRepository.existsByNameAndSector("멋사 핫도그", BoothSector.한글탑)).thenReturn(false);
        when(boothRepository.save(any(Booth.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint uq_booths_admin_id"));

        assertThatThrownBy(() -> boothService.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.DUPLICATE_BOOTH_ADMIN));
    }

    @Test
    @DisplayName("부스 이름 UNIQUE 제약 위반(admin_id 중복 아님)은 DUPLICATE_BOOTH_NAME 으로 매핑된다")
    void createMapsNameUniqueViolationToDuplicateBoothName() {
        when(adminUserRepository.existsById(1L)).thenReturn(true);
        // admin_id 는 선검증·재확인 모두 중복 아님 → 이름 제약 위반으로 판정
        when(boothRepository.existsByAdminId(1L)).thenReturn(false, false);
        when(boothRepository.existsByNameAndSector("멋사 핫도그", BoothSector.한글탑)).thenReturn(false);
        when(boothRepository.save(any(Booth.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint uq_booths_name"));

        assertThatThrownBy(() -> boothService.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.DUPLICATE_BOOTH_NAME));
    }

    @Test
    @DisplayName("대표 메뉴 콤마결합 길이가 255자를 초과하면 수정은 INVALID_INPUT 으로 거절한다 (representative_menus VARCHAR(255) 오버플로우 방지)")
    void updateRejectsRepresentativeMenusOverColumnLimit() {
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth(7L, null)));

        BoothUpdateRequest request = updateRequestWithMenus(List.of("메".repeat(256)));

        assertThatThrownBy(() -> boothService.update(7L, request, boothAdmin(1L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("대표 메뉴 콤마결합 길이가 255자를 초과하면 생성은 INVALID_INPUT 으로 거절하고 저장하지 않는다")
    void createRejectsRepresentativeMenusOverColumnLimit() {
        when(adminUserRepository.existsById(1L)).thenReturn(true);
        when(boothRepository.existsByAdminId(1L)).thenReturn(false);
        when(boothRepository.existsByNameAndSector("멋사 핫도그", BoothSector.한글탑)).thenReturn(false);
        lenient().when(boothRepository.save(any(Booth.class))).thenAnswer(invocation -> {
            Booth saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        BoothCreateRequest request = createRequestWithMenus(List.of("메".repeat(256)));

        assertThatThrownBy(() -> boothService.create(request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));

        verify(boothRepository, never()).save(any(Booth.class));
    }

    @Test
    @DisplayName("생성 시 이름 검증을 전역이 아니라 (이름, 구역) 범위로 수행한다 — 다른 구역 동명은 영향 없음")
    void createChecksNameUniquenessScopedToSector() {
        when(adminUserRepository.existsById(1L)).thenReturn(true);
        when(boothRepository.existsByAdminId(1L)).thenReturn(false);
        // 요청 구역(한글탑)엔 같은 이름이 없음 → 허용. 다른 구역에 동명이 있어도 이 조회로는 안 잡힌다.
        when(boothRepository.existsByNameAndSector("멋사 핫도그", BoothSector.한글탑)).thenReturn(false);
        when(boothRepository.save(any(Booth.class))).thenAnswer(invocation -> {
            Booth saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        BoothResponse response = boothService.create(createRequest()); // name "멋사 핫도그", sector 한글탑

        assertThat(response.getName()).isEqualTo("멋사 핫도그");
        // 전역 existsByName 이 아니라 구역 범위 existsByNameAndSector 로 검증해야 함을 못박는다.
        // (이게 깨지면 = 전역 유일로 회귀 = 다른 구역 동명을 잘못 거절)
        verify(boothRepository).existsByNameAndSector("멋사 핫도그", BoothSector.한글탑);
        verify(boothRepository, never()).existsByName(anyString());
    }

    @Test
    @DisplayName("같은 구역에 같은 이름 부스가 있으면 생성이 DUPLICATE_BOOTH_NAME 으로 거절된다")
    void createRejectsSameNameInSameSector() {
        when(adminUserRepository.existsById(1L)).thenReturn(true);
        when(boothRepository.existsByAdminId(1L)).thenReturn(false);
        when(boothRepository.existsByNameAndSector("멋사 핫도그", BoothSector.한글탑)).thenReturn(true);

        assertThatThrownBy(() -> boothService.create(createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.DUPLICATE_BOOTH_NAME));

        verify(boothRepository, never()).save(any(Booth.class));
    }

    @Test
    @DisplayName("수정 시 같은 구역에 같은 이름 부스가 있으면 DUPLICATE_BOOTH_NAME 으로 거절된다 (본인 제외)")
    void updateRejectsSameNameInSameSector() {
        Booth booth = booth(7L, null); // adminId 1L, name "멋사 핫도그", sector 한글탑
        when(boothRepository.findById(7L)).thenReturn(Optional.of(booth));
        when(boothRepository.existsByNameAndSectorAndIdNot("멋사 핫도그", BoothSector.한글탑, 7L)).thenReturn(true);

        assertThatThrownBy(() -> boothService.update(7L, updateRequest(), boothAdmin(1L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoothErrorCode.DUPLICATE_BOOTH_NAME));
    }

    private AdminSessionUser superAdmin() {
        return new AdminSessionUser(99L, AdminRole.SUPER, "super");
    }

    private AdminSessionUser boothAdmin(Long id) {
        return new AdminSessionUser(id, AdminRole.BOOTH, "booth-" + id);
    }

    // 부스 픽스처(name "멋사 핫도그", openTime 11:00, closeTime 20:00)와 동일 — name 동일이라 중복검사 단락, 시간검증 통과.
    private BoothUpdateRequest updateRequest() {
        return new BoothUpdateRequest(
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
                null,
                null,
                false,
                null
        );
    }

    // 부스 픽스처와 동일(adminId 1L, name "멋사 핫도그", openTime 11:00, closeTime 20:00) — 시간검증 통과.
    private BoothCreateRequest createRequest() {
        return new BoothCreateRequest(
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
                null,
                null,
                false,
                null
        );
    }

    private BoothUpdateRequest updateRequestWithMenus(List<String> representativeMenus) {
        return new BoothUpdateRequest(
                "멋사 핫도그", "멋쟁이사자처럼", "소개", 2,
                LocalTime.of(11, 0), LocalTime.of(20, 0), BoothSector.한글탑, 3,
                BoothStatus.OPEN, true, null, true, null, null,
                representativeMenus, false, null
        );
    }

    private BoothCreateRequest createRequestWithMenus(List<String> representativeMenus) {
        return new BoothCreateRequest(
                1L, "멋사 핫도그", "멋쟁이사자처럼", "소개", 2,
                LocalTime.of(11, 0), LocalTime.of(20, 0), BoothSector.한글탑, 3,
                BoothStatus.OPEN, true, null, true, null, null,
                representativeMenus, false, null
        );
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
