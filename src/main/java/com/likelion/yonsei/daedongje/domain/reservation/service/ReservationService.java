package com.likelion.yonsei.daedongje.domain.reservation.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCancelRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationResponse;
import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.exception.ReservationErrorCode;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BoothRepository boothRepository;

    // 예약 생성
    // 부스 행에 비관적 락을 걸어 부스별 예약 순번 중복을 방지한다.
    @Transactional
    public ReservationResponse create(Long boothId, ReservationCreateRequest request) {
        Booth booth = boothRepository.findByIdWithLock(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!booth.getIsReservable()) {
            throw new BusinessException(ReservationErrorCode.BOOTH_NOT_RESERVABLE);
        }

        int nextNumber = reservationRepository.findMaxReservationNumberByBoothId(boothId)
                .map(max -> max + 1)
                .orElse(1);

        Reservation reservation = Reservation.create(
                booth,
                nextNumber,
                request.bookerName(),
                request.phoneNumber(),
                request.partySize(),
                request.pin()
        );

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    // 예약 단건 조회
    public ReservationResponse getById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        return ReservationResponse.from(reservation);
    }

    // 부스별 예약 목록 조회 (status 파라미터로 상태 필터링)
    public List<ReservationResponse> getListByBooth(Long boothId, ReservationStatus status) {
        List<Reservation> reservations = (status != null)
                ? reservationRepository.findAllByBoothIdAndStatus(boothId, status)
                : reservationRepository.findAllByBoothId(boothId);

        return reservations.stream()
                .map(ReservationResponse::from)
                .toList();
    }

    // 사용자 예약 목록 조회 (이름 + 연락처 + 선택적 PIN + 선택적 상태 필터)
    public List<ReservationResponse> getListByBooker(String bookerName, String phoneNumber,
                                                     String pin, ReservationStatus status) {
        List<Reservation> reservations =
                reservationRepository.findAllByBookerNameAndPhoneNumber(bookerName, phoneNumber);

        return reservations.stream()
                .filter(r -> r.matchesPin(pin))
                .filter(r -> status == null || r.getStatus() == status)
                .map(ReservationResponse::from)
                .toList();
    }

    // 예약 입장 처리 (PENDING → CONFIRMED)
    @Transactional
    public ReservationResponse confirm(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(ReservationErrorCode.CANNOT_CONFIRM_CANCELLED);
        }

        reservation.confirm();
        return ReservationResponse.from(reservation);
    }

    // 예약 취소
    @Transactional
    public ReservationResponse cancel(Long id, ReservationCancelRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(ReservationErrorCode.ALREADY_CANCELLED);
        }

        reservation.cancel(request.cancelReason());
        return ReservationResponse.from(reservation);
    }
}
