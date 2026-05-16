package com.likelion.yonsei.daedongje.domain.reservation.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationAdminStatusRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationResponse;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationUserCancelRequest;
import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.exception.ReservationErrorCode;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BoothRepository boothRepository;
    private final PasswordEncoder passwordEncoder;

    // 예약 생성
    // 부스 행에 비관적 락을 걸어 부스별 예약 순번 중복을 방지한다.
    @Transactional
    public ReservationCreateResponse create(Long boothId, ReservationCreateRequest request) {
        Booth booth = boothRepository.findByIdWithLock(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!booth.getIsReservable()) {
            throw new BusinessException(ReservationErrorCode.BOOTH_NOT_RESERVABLE);
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
    public List<ReservationResponse> getListByBooker(String bookerName, String phoneNumber,
                                                     String pin, ReservationStatus status) {
        return reservationRepository
                .findAllByBookerNameAndPhoneNumberWithFilter(bookerName, phoneNumber, status)
                .stream()
                .filter(r -> pinMatches(r.getPin(), pin))
                .map(ReservationResponse::from)
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

    // PIN 없는 예약은 항상 통과, PIN 있는 예약은 BCrypt matches로 검증
    private boolean pinMatches(String storedPin, String providedPin) {
        if (storedPin == null) return true;
        if (providedPin == null) return false;
        return passwordEncoder.matches(providedPin, storedPin);
    }
}
