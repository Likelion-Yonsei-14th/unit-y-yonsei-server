package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BoothService {

    private final BoothRepository boothRepository;

    public BoothService(BoothRepository boothRepository) {
        this.boothRepository = boothRepository;
    }

    // 부스 생성
    @Transactional
    public BoothResponse create(BoothCreateRequest request) {
        if (boothRepository.existsByName(request.name())) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
        if (request.closeTime().isBefore(request.openTime()) ||
                request.closeTime().equals(request.openTime())) {
            throw new BusinessException(BoothErrorCode.INVALID_BOOTH_TIME);
        }

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

        return BoothResponse.from(boothRepository.save(booth));
    }

    // 부스 단건 조회
    public BoothResponse getById(Long id) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));
        return BoothResponse.from(booth);
    }

    // 부스 전체 조회 (필터: 날짜, 구역, 음식 여부)
    public List<BoothResponse> getList(Integer date, String sector, Boolean isFood) {
        List<Booth> booths;

        if (date != null && sector != null) {
            booths = boothRepository.findAllByDateAndSector(date, sector);
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

    // 부스 수정
    @Transactional
    public BoothResponse update(Long id, BoothUpdateRequest request) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (!booth.getName().equals(request.name()) &&
                boothRepository.existsByName(request.name())) {
            throw new BusinessException(BoothErrorCode.DUPLICATE_BOOTH_NAME);
        }
        if (request.closeTime().isBefore(request.openTime()) ||
                request.closeTime().equals(request.openTime())) {
            throw new BusinessException(BoothErrorCode.INVALID_BOOTH_TIME);
        }

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
    }

    // 부스 삭제
    @Transactional
    public void delete(Long id) {
        Booth booth = boothRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));
        boothRepository.delete(booth);
    }
}
