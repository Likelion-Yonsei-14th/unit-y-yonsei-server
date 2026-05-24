package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
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
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothImageRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.MenuRepository;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
import com.likelion.yonsei.daedongje.domain.map.dto.MapLocationResponse;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import com.likelion.yonsei.daedongje.common.response.PageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BoothService {

    private final BoothRepository boothRepository;
    private final BoothImageRepository boothImageRepository;
    private final BoothClickLogRepository boothClickLogRepository;
    private final ReservationRepository reservationRepository;
    private final MapLocationRepository mapLocationRepository;
    private final MenuRepository menuRepository;
    private final NoticeRepository noticeRepository;
    private final AdminUserRepository adminUserRepository;

    public BoothService(BoothRepository boothRepository, BoothImageRepository boothImageRepository,
                        BoothClickLogRepository boothClickLogRepository,
                        ReservationRepository reservationRepository, MapLocationRepository mapLocationRepository,
                        MenuRepository menuRepository, NoticeRepository noticeRepository,
                        AdminUserRepository adminUserRepository) {
        this.boothRepository = boothRepository;
        this.boothImageRepository = boothImageRepository;
        this.boothClickLogRepository = boothClickLogRepository;
        this.reservationRepository = reservationRepository;
        this.mapLocationRepository = mapLocationRepository;
        this.menuRepository = menuRepository;
        this.noticeRepository = noticeRepository;
        this.adminUserRepository = adminUserRepository;
    }

    // 부스 생성 (담당 어드민 검증: 존재하는 계정인지 + 계정당 부스 1개 정책)
    @Transactional
    public BoothResponse create(BoothCreateRequest request) {
        // 존재하지 않는 어드민에 부스가 묶이면 고아 부스가 되므로 차단
        if (!adminUserRepository.existsById(request.adminId())) {
            throw new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND);
        }
        // 계정당 부스 1개 정책 — 이미 담당 부스가 있는 어드민이면 차단
        if (boothRepository.existsByAdminId(request.adminId())) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_ADMIN);
        }
        if (boothRepository.existsByName(request.name())) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
        validateBoothTime(request.openTime(), request.closeTime());

        Booth booth = Booth.create(
                request.adminId(),
                request.name(),
                request.organization(),
                request.description(),
                request.date(),
                request.openTime(),
                request.closeTime(),
                request.sector(),
                request.location(),
                request.status(),
                request.isFood(),
                request.instagram(),
                request.isReservable(),
                request.account(),
                request.locationId(),
                toMenuString(request.representativeMenus()),
                request.isFoodTruck(),
                request.notice()
        );

        try {
            return BoothResponse.from(boothRepository.save(booth));
        } catch (DataIntegrityViolationException e) {
            // 선검증(existsBy…)과 INSERT 사이의 race(TOCTOU) 로 UNIQUE 제약에 걸린 경우 —
            // admin_id(계정당 부스 1개) 와 name 중 어느 제약 위반인지 재확인해 매핑한다.
            if (boothRepository.existsByAdminId(request.adminId())) {
                throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_ADMIN);
            }
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
    }

    // 부스 단건 조회
    public BoothResponse getById(Long id) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));
        return BoothResponse.of(booth, 0L, fetchThumbnail(id), fetchMapLocation(booth));
    }

    // 부스 전체 조회 (필터: 날짜·구역·음식 여부·푸드트럭 여부·운영상태 AND, 페이지네이션)
    public PageResponse<BoothResponse> getList(Integer date, BoothSector sector, Boolean isFood,
                                               Boolean isFoodTruck, BoothStatus status, int page, int size) {
        List<Booth> booths;

        if (date != null && sector != null && isFood != null) {
            booths = boothRepository.findAllByDateAndSectorAndIsFood(date, sector, isFood);
        } else if (date != null && sector != null) {
            booths = boothRepository.findAllByDateAndSector(date, sector);
        } else if (date != null && isFood != null) {
            booths = boothRepository.findAllByDateAndIsFood(date, isFood);
        } else if (sector != null && isFood != null) {
            booths = boothRepository.findAllBySectorAndIsFood(sector, isFood);
        } else if (date != null) {
            booths = boothRepository.findAllByDate(date);
        } else if (sector != null) {
            booths = boothRepository.findAllBySector(sector);
        } else if (isFood != null) {
            booths = boothRepository.findAllByIsFood(isFood);
        } else {
            booths = boothRepository.findAll();
        }

        if (isFoodTruck != null) {
            booths = booths.stream()
                    .filter(b -> b.getIsFoodTruck().equals(isFoodTruck))
                    .toList();
        }

        if (status != null) {
            booths = booths.stream()
                    .filter(b -> b.getStatus() == status)
                    .toList();
        }

        return paginate(booths, page, size);
    }

    // 부스명·단체명·메뉴명 키워드 검색 (페이지네이션)
    public PageResponse<BoothResponse> search(String keyword, int page, int size) {
        return paginate(boothRepository.searchByKeyword(keyword), page, size);
    }

    // 필터링된 부스 목록을 id 오름차순으로 정렬해 인메모리 페이지네이션한다.
    // (부스 수가 한정적이라 인메모리 슬라이스로 충분하며, 기존 다중 필터 메서드를 그대로 재사용한다.)
    private PageResponse<BoothResponse> paginate(List<Booth> booths, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        List<Booth> ordered = booths.stream().sorted(Comparator.comparing(Booth::getId)).toList();
        int from = Math.min(safePage * safeSize, ordered.size());
        int to = Math.min(from + safeSize, ordered.size());
        List<BoothResponse> content = toBoothResponses(ordered.subList(from, to));

        return PageResponse.from(new PageImpl<>(content, pageable, ordered.size()));
    }

    // 부스 목록을 응답으로 변환 — 대기 팀 수 일괄 집계·썸네일·지도 위치를 한 번에 매핑한다.
    private List<BoothResponse> toBoothResponses(List<Booth> booths) {
        if (booths.isEmpty()) return List.of();

        List<Long> boothIds = booths.stream().map(Booth::getId).toList();
        Map<Long, Long> waitingCountMap = reservationRepository
                .countByBoothIdsAndStatus(boothIds, ReservationStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        Map<Long, String> thumbnailMap = fetchThumbnailMap(boothIds);
        Map<Long, MapLocationResponse> mapLocationMap = fetchMapLocationMap(booths);

        return booths.stream()
                .map(booth -> BoothResponse.of(booth, waitingCountMap.getOrDefault(booth.getId(), 0L), thumbnailMap.get(booth.getId()), resolveMapLocation(booth, mapLocationMap)))
                .toList();
    }

    // 예약 가능 부스 목록 조회 (isReservable=true AND status=OPEN, 대기 팀 수 포함)
    public List<ReservableBoothResponse> getReservableList() {
        List<Booth> booths = boothRepository.findAllByIsReservableAndStatus(true, BoothStatus.OPEN);
        if (booths.isEmpty()) return List.of();

        List<Long> boothIds = booths.stream().map(Booth::getId).toList();
        Map<Long, Long> waitingCountMap = reservationRepository
                .countByBoothIdsAndStatus(boothIds, ReservationStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        Map<Long, String> thumbnailMap = fetchThumbnailMap(boothIds);

        return booths.stream()
                .map(booth -> ReservableBoothResponse.of(
                        booth,
                        waitingCountMap.getOrDefault(booth.getId(), 0L),
                        thumbnailMap.get(booth.getId())
                ))
                .toList();
    }

    // 부스 수정 (SUPER 외 역할은 본인 담당 부스만 수정 가능)
    @Transactional
    public BoothResponse update(Long id, BoothUpdateRequest request, AdminSessionUser currentAdmin) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!currentAdmin.isSuper() && !booth.getAdminId().equals(currentAdmin.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        if (!booth.getName().equals(request.name()) &&
                boothRepository.existsByName(request.name())) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
        validateBoothTime(request.openTime(), request.closeTime());

        try {
            booth.update(
                    request.name(),
                    request.organization(),
                    request.description(),
                    request.date(),
                    request.openTime(),
                    request.closeTime(),
                    request.sector(),
                    request.location(),
                    request.status(),
                    request.isFood(),
                    request.instagram(),
                    request.isReservable(),
                    request.account(),
                    request.locationId(),
                    toMenuString(request.representativeMenus()),
                    request.isFoodTruck(),
                    request.notice()
            );
            return BoothResponse.of(booth, 0L, fetchThumbnail(booth.getId()), fetchMapLocation(booth));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
    }

    // 부스 운영 상태 변경 (SUPER 외 역할은 본인 담당 부스만 변경 가능)
    @Transactional
    public BoothResponse updateStatus(Long id, BoothStatus status, AdminSessionUser currentAdmin) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!currentAdmin.isSuper() && !booth.getAdminId().equals(currentAdmin.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        booth.updateStatus(status);
        return BoothResponse.of(booth, 0L, fetchThumbnail(booth.getId()), fetchMapLocation(booth));
    }

    // 예약 접수 On/Off (SUPER 외 역할은 본인 담당 부스만 변경 가능)
    @Transactional
    public BoothResponse updateIsReservable(Long id, boolean isReservable, AdminSessionUser currentAdmin) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!currentAdmin.isSuper() && !booth.getAdminId().equals(currentAdmin.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        booth.updateIsReservable(isReservable);
        return BoothResponse.of(booth, 0L, fetchThumbnail(booth.getId()), fetchMapLocation(booth));
    }

    // 부스 삭제 — 운영 데이터(예약·메뉴·공지) 가 남아 있으면 차단해 실수 삭제 방지 (BAC-109).
    // 분석·코스메틱 자식(booth_images / booth_click_logs) 은 사용자 입력이 아니므로 차단하지 않고
    // application-level cascade 로 명시적으로 정리한다 — 운영 DB 의 FK 가 ON DELETE CASCADE
    // 가 아닌 환경에서도 동일하게 동작하기 위한 안전망 (BAC-111).
    @Transactional
    public void delete(Long id, AdminSessionUser currentAdmin) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!currentAdmin.isSuper() && !booth.getAdminId().equals(currentAdmin.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        verifyNoChildData(id);

        // 분석·코스메틱 자식 application-level cascade — DB FK 의 ON DELETE 동작과 무관하게 항상 정리됨 (BAC-111).
        boothClickLogRepository.deleteByBoothId(id);
        boothImageRepository.deleteByBoothId(id);

        try {
            boothRepository.delete(booth);
            // FK 검증을 트랜잭션 커밋이 아닌 *여기서* 실행 — 아래 catch 로 잡을 수 있게 함
            boothRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 검사 시점과 삭제 시점 사이 race — 자식 데이터가 새로 생성됐을 가능성.
            // 어떤 자식이 막혔는지 다시 확인해 의미 있는 BusinessException 으로 변환한다.
            verifyNoChildData(id);
            throw e;  // 알려진 자식이 아니면 원본 FK 위반을 유지 (다른 미지의 FK)
        }
    }

    private void verifyNoChildData(Long boothId) {
        if (reservationRepository.existsByBoothId(boothId)) {
            throw new BusinessException(BoothErrorCode.BOOTH_HAS_RESERVATIONS);
        }
        if (menuRepository.existsByBoothId(boothId)) {
            throw new BusinessException(BoothErrorCode.BOOTH_HAS_MENUS);
        }
        if (noticeRepository.existsByBoothId(boothId)) {
            throw new BusinessException(BoothErrorCode.BOOTH_HAS_NOTICES);
        }
    }

    private String fetchThumbnail(Long boothId) {
        return boothImageRepository.findByBoothIdAndDisplayOrder(boothId, 1)
                .map(BoothImage::getImageUrl)
                .orElse(null);
    }

    private Map<Long, String> fetchThumbnailMap(List<Long> boothIds) {
        return boothImageRepository.findThumbnailsByBoothIds(boothIds).stream()
                .collect(Collectors.toMap(BoothImage::getBoothId, BoothImage::getImageUrl));
    }

    private MapLocationResponse fetchMapLocation(Booth booth) {
        if (booth.getLocationId() == null) return null;
        return mapLocationRepository.findById(booth.getLocationId())
                .map(MapLocationResponse::from)
                .orElse(null);
    }

    private Map<Long, MapLocationResponse> fetchMapLocationMap(List<Booth> booths) {
        List<Long> locationIds = booths.stream()
                .map(Booth::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (locationIds.isEmpty()) return Map.of();
        return mapLocationRepository.findAllById(locationIds).stream()
                .collect(Collectors.toMap(MapLocation::getId, MapLocationResponse::from));
    }

    /**
     * 부스의 지도 위치를 맵에서 조회한다.
     * locationId 가 null 이면 위치 미지정이므로 null 을 반환한다.
     * mapLocationMap 이 비어 있을 때는 Map.of() 불변 맵이라 null 키로 get 하면 NPE 가 발생하므로,
     * locationId null 검사를 먼저 해 .get(null) 호출 자체를 막는다.
     */
    private MapLocationResponse resolveMapLocation(Booth booth, Map<Long, MapLocationResponse> mapLocationMap) {
        Long locationId = booth.getLocationId();
        if (locationId == null) return null;
        return mapLocationMap.get(locationId);
    }

    private String toMenuString(List<String> menus) {
        if (menus == null || menus.isEmpty()) return null;
        return String.join(",", menus);
    }

    /**
     * 운영 시간 유효성 검사.
     * - openTime, closeTime 둘 중 하나만 입력된 경우 예외 (둘 다 null이거나 둘 다 non-null이어야 함)
     * - 둘 다 입력된 경우 closeTime > openTime이어야 함
     */
    private void validateBoothTime(LocalTime openTime, LocalTime closeTime) {
        boolean openProvided = openTime != null;
        boolean closeProvided = closeTime != null;
        if (openProvided != closeProvided) {
            throw new BusinessException(BoothErrorCode.INVALID_BOOTH_TIME);
        }
        if (openProvided && (closeTime.isBefore(openTime) || closeTime.equals(openTime))) {
            throw new BusinessException(BoothErrorCode.INVALID_BOOTH_TIME);
        }
    }
}
