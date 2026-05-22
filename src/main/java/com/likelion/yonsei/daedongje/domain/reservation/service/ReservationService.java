package com.likelion.yonsei.daedongje.domain.reservation.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothImageRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.reservation.dto.MyReservationResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.MyReservationResponse.BoothInfo;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationAdminStatusRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationSummaryResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationSummaryResponse.BoothSummary;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationSummaryResponse.Totals;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationUpdateRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationUserCancelRequest;
import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.exception.ReservationErrorCode;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BoothRepository boothRepository;
    private final BoothImageRepository boothImageRepository;
    private final PasswordEncoder passwordEncoder;

    /** 같은 전화번호의 동일 부스 예약을 광클로 간주해 멱등 처리하는 시간 윈도우. application.yaml 의 app.reservation.duplicate-window 로 조정. */
    @Value("${app.reservation.duplicate-window:10s}")
    private Duration duplicateWindow;

    // 예약 생성
    // 부스 행에 비관적 락을 걸어 부스별 예약 순번 중복을 방지한다.
    @Transactional
    public ReservationCreateResponse create(Long boothId, ReservationCreateRequest request) {
        Booth booth = boothRepository.findByIdWithLock(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!booth.getIsReservable()) {
            throw new BusinessException(ReservationErrorCode.BOOTH_NOT_RESERVABLE);
        }

        // 광클 멱등 처리: 같은 전화번호로 최근 duplicateWindow 안에 동일 부스 PENDING 예약이 있으면
        // 신규 생성 없이 그 예약을 그대로 반환한다. 부스 비관적 락이 create 를 직렬화하므로 경합은 없다.
        LocalDateTime since = LocalDateTime.now().minus(duplicateWindow);
        Optional<Reservation> existing = reservationRepository
                .findFirstByBooth_IdAndPhoneNumberAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        boothId, request.phoneNumber(), ReservationStatus.PENDING, since);
        if (existing.isPresent()) {
            Reservation found = existing.get();
            // 쿼리가 status=PENDING 으로 필터링한 결과라 found 는 PENDING 카운트에 반드시 포함된다.
            // 본인(found)을 빼기 위해 -1 — 신규 생성 경로의 aheadOfMe 와 동일한 규칙.
            long aheadOfExisting =
                    reservationRepository.countByBoothIdAndStatus(boothId, ReservationStatus.PENDING) - 1;
            return ReservationCreateResponse.of(found, aheadOfExisting);
        }

        int nextNumber = reservationRepository.findMaxReservationNumberByBoothId(boothId)
                .map(max -> max + 1)
                .orElse(1);

        String hashedPin = (request.pin() != null) ? passwordEncoder.encode(request.pin()) : null;
        Reservation reservation = Reservation.create(
                booth,
                nextNumber,
                request.bookerName(),
                request.phoneNumber(),
                request.partySize(),
                hashedPin
        );

        Reservation saved = reservationRepository.save(reservation);
        long aheadOfMe = reservationRepository.countByBoothIdAndStatus(boothId, ReservationStatus.PENDING) - 1;
        return ReservationCreateResponse.of(saved, aheadOfMe);
    }

    // 부스별 예약 목록 조회 (status 파라미터로 상태 필터링)
    // SUPER: 모든 부스 조회 가능 / BOOTH: 본인 담당 부스만 조회 가능
    public List<ReservationResponse> getListByBooth(Long boothId, ReservationStatus status,
                                                    AdminSessionUser currentAdmin) {
        if (!currentAdmin.isSuper()) {
            verifyBoothOwnership(boothId, currentAdmin);
        }

        List<Reservation> reservations = (status != null)
                ? reservationRepository.findAllByBoothIdAndStatus(boothId, status)
                : reservationRepository.findAllByBoothId(boothId);

        return reservations.stream()
                .map(ReservationResponse::from)
                .toList();
    }

    // 부스별 예약 현황 요약 조회 (대기/완료/취소 카운트 + 전체 합산)
    // SUPER·MASTER: 모든 부스 / BOOTH: 본인 담당 부스만
    public ReservationSummaryResponse getSummary(AdminSessionUser currentAdmin) {
        List<Long> boothIds = resolveAccessibleBoothIds(currentAdmin);
        if (boothIds.isEmpty()) {
            return ReservationSummaryResponse.of(List.of(), buildTotals(List.of()));
        }

        // 부스 ID 오름차순으로 정렬된 상태별 카운트 맵 구성
        Map<Long, Map<ReservationStatus, Long>> countsByBooth = new TreeMap<>();
        for (Object[] row : reservationRepository.countGroupedByBoothIdAndStatus(boothIds)) {
            Long boothId = (Long) row[0];
            ReservationStatus status = (ReservationStatus) row[1];
            long count = (Long) row[2];
            countsByBooth
                    .computeIfAbsent(boothId, key -> new EnumMap<>(ReservationStatus.class))
                    .put(status, count);
        }

        List<BoothSummary> booths = countsByBooth.entrySet().stream()
                .map(entry -> toBoothSummary(entry.getKey(), entry.getValue()))
                .toList();

        return ReservationSummaryResponse.of(booths, buildTotals(booths));
    }

    // 어드민 예약 단건 조회
    // SUPER: 모든 예약 조회 가능 / BOOTH: 본인 담당 부스의 예약만 조회 가능
    public ReservationResponse getByIdForAdmin(Long id, AdminSessionUser currentAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (!currentAdmin.isSuper()) {
            verifyBoothOwnership(reservation.getBooth().getId(), currentAdmin);
        }

        return ReservationResponse.from(reservation);
    }

    // 사용자 예약 목록 조회 (이름 + 연락처 + 선택적 PIN + 선택적 상태 필터)
    // PIN이 있는 예약은 BCrypt 비교로 필터링
    public List<MyReservationResponse> getListByBooker(String bookerName, String phoneNumber,
                                                       String pin, ReservationStatus status) {
        List<Reservation> reservations = reservationRepository
                .findAllByBookerNameAndPhoneNumberWithFilter(bookerName, phoneNumber, status)
                .stream()
                .filter(r -> pinMatches(r.getPin(), pin))
                .toList();

        if (reservations.isEmpty()) {
            return List.of();
        }

        List<Long> boothIds = reservations.stream()
                .map(r -> r.getBooth().getId())
                .distinct()
                .toList();

        Map<Long, String> thumbnailByBoothId = boothImageRepository.findThumbnailsByBoothIds(boothIds)
                .stream()
                .collect(Collectors.toMap(BoothImage::getBoothId, BoothImage::getImageUrl, (a, b) -> a));

        Map<Long, Long> waitingCountByBoothId = reservationRepository
                .countByBoothIdsAndStatus(boothIds, ReservationStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, List<Integer>> pendingNumbersByBoothId = reservationRepository
                .findBoothIdAndReservationNumbersByBoothIdsAndStatus(boothIds, ReservationStatus.PENDING)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (Integer) row[1], Collectors.toList())
                ));

        return reservations.stream()
                .map(r -> {
                    Long boothId = r.getBooth().getId();
                    long aheadOfMe = r.getStatus() != ReservationStatus.PENDING ? 0L
                            : pendingNumbersByBoothId.getOrDefault(boothId, List.of())
                                    .stream()
                                    .filter(num -> num < r.getReservationNumber())
                                    .count();
                    BoothInfo boothInfo = BoothInfo.of(
                            r.getBooth(),
                            waitingCountByBoothId.getOrDefault(boothId, 0L),
                            thumbnailByBoothId.get(boothId)
                    );
                    return MyReservationResponse.of(r, aheadOfMe, boothInfo);
                })
                .toList();
    }

    // 어드민 예약 상태 변경 (CONFIRMED: 입장 처리 / CANCELLED: 취소)
    // SUPER: 모든 예약 변경 가능 / BOOTH: 본인 담당 부스의 예약만 변경 가능
    @Transactional
    public ReservationResponse updateStatusByAdmin(Long id, ReservationAdminStatusRequest request,
                                                   AdminSessionUser currentAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (!currentAdmin.isSuper()) {
            verifyBoothOwnership(reservation.getBooth().getId(), currentAdmin);
        }

        switch (request.status()) {
            case CONFIRMED -> {
                if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                    throw new BusinessException(ReservationErrorCode.CANNOT_CONFIRM_CANCELLED);
                }
                reservation.confirm();
            }
            case CANCELLED -> {
                if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                    throw new BusinessException(ReservationErrorCode.ALREADY_CANCELLED);
                }
                reservation.cancel();
            }
            default -> throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
        }

        return ReservationResponse.from(reservation);
    }

    // 사용자 본인 예약 정보 수정 (이름 + 연락처 + PIN으로 소유권 확인, PENDING 상태만 가능)
    @Transactional
    public ReservationResponse updateByBooker(Long id, ReservationUpdateRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getBookerName().equals(request.bookerName()) ||
                !reservation.getPhoneNumber().equals(request.phoneNumber()) ||
                !pinMatches(reservation.getPin(), request.pin())) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(ReservationErrorCode.CANNOT_UPDATE_NON_PENDING);
        }

        reservation.update(request.newBookerName(), request.newPhoneNumber(), request.newPartySize());
        return ReservationResponse.from(reservation);
    }

    // 사용자 본인 예약 취소 (이름 + 연락처 + PIN으로 소유권 확인)
    @Transactional
    public ReservationResponse cancelByBooker(Long id, ReservationUserCancelRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 소유권 확인 (보안상 예약 미존재와 동일한 에러 반환)
        if (!reservation.getBookerName().equals(request.bookerName()) ||
                !reservation.getPhoneNumber().equals(request.phoneNumber()) ||
                !pinMatches(reservation.getPin(), request.pin())) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(ReservationErrorCode.ALREADY_CANCELLED);
        }

        reservation.cancel();
        return ReservationResponse.from(reservation);
    }

    // BOOTH 어드민의 부스 소유권 검증
    // 담당 부스가 아니면 403 반환
    private void verifyBoothOwnership(Long boothId, AdminSessionUser currentAdmin) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!booth.getAdminId().equals(currentAdmin.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }
    }

    // 예약 현황 요약을 조회할 수 있는 부스 ID 목록
    // BOOTH 권한은 본인 담당 부스만, SUPER·MASTER는 전체 부스
    private List<Long> resolveAccessibleBoothIds(AdminSessionUser currentAdmin) {
        List<Booth> booths = currentAdmin.hasRole(AdminRole.BOOTH)
                ? boothRepository.findAllByAdminIdIn(List.of(currentAdmin.getId()))
                : boothRepository.findAll();
        return booths.stream()
                .map(Booth::getId)
                .toList();
    }

    // 상태별 카운트 맵을 부스 현황 DTO로 변환 (해당 상태 행이 없으면 0)
    private BoothSummary toBoothSummary(Long boothId, Map<ReservationStatus, Long> counts) {
        long pending = counts.getOrDefault(ReservationStatus.PENDING, 0L);
        long confirmed = counts.getOrDefault(ReservationStatus.CONFIRMED, 0L);
        long cancelled = counts.getOrDefault(ReservationStatus.CANCELLED, 0L);
        return BoothSummary.builder()
                .boothId(boothId)
                .pending(pending)
                .confirmed(confirmed)
                .cancelled(cancelled)
                .total(pending + confirmed + cancelled)
                .build();
    }

    // 부스별 현황을 모두 합산해 전체 합계 DTO 생성
    private Totals buildTotals(List<BoothSummary> booths) {
        long pending = booths.stream().mapToLong(BoothSummary::getPending).sum();
        long confirmed = booths.stream().mapToLong(BoothSummary::getConfirmed).sum();
        long cancelled = booths.stream().mapToLong(BoothSummary::getCancelled).sum();
        return Totals.builder()
                .pending(pending)
                .confirmed(confirmed)
                .cancelled(cancelled)
                .total(pending + confirmed + cancelled)
                .build();
    }

    // PIN 없는 예약은 항상 통과, PIN 있는 예약은 BCrypt matches로 검증
    private boolean pinMatches(String storedPin, String providedPin) {
        if (storedPin == null) return true;
        if (providedPin == null) return false;
        return passwordEncoder.matches(providedPin, storedPin);
    }
}
