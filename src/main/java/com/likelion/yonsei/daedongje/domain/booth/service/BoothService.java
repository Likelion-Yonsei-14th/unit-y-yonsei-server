package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.ReservableBoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BoothService {

    private final BoothRepository boothRepository;
    private final ReservationRepository reservationRepository;

    public BoothService(BoothRepository boothRepository, ReservationRepository reservationRepository) {
        this.boothRepository = boothRepository;
        this.reservationRepository = reservationRepository;
    }

    // 부스 생성
    @Transactional
    public BoothResponse create(BoothCreateRequest request) {
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
                request.locationId()
        );

        try {
            return BoothResponse.from(boothRepository.save(booth));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
    }

    // 부스 단건 조회
    public BoothResponse getById(Long id) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));
        return BoothResponse.from(booth);
    }

    // 부스 전체 조회 (필터: 날짜, 구역, 음식 여부 — 모든 AND 조합 지원)
    public List<BoothResponse> getList(Integer date, BoothSector sector, Boolean isFood) {
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

        return booths.stream()
                .map(BoothResponse::from)
                .toList();
    }

    // 예약 가능 부스 목록 조회 (대기 팀 수 포함)
    public List<ReservableBoothResponse> getReservableList() {
        return boothRepository.findAllByIsReservable(true).stream()
                .map(booth -> ReservableBoothResponse.of(
                        booth,
                        reservationRepository.countByBoothIdAndStatus(booth.getId(), ReservationStatus.PENDING)
                ))
                .toList();
    }

    // 부스 수정
    @Transactional
    public BoothResponse update(Long id, BoothUpdateRequest request) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

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
                    request.locationId()
            );
            return BoothResponse.from(booth);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
    }

    // 부스 삭제
    @Transactional
    public void delete(Long id) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));
        boothRepository.delete(booth);
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
