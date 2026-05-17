package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothClickLog;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class BoothClickLogService {

    private final BoothRepository boothRepository;
    private final BoothClickLogRepository boothClickLogRepository;

    public BoothClickLogService(BoothRepository boothRepository,
                                BoothClickLogRepository boothClickLogRepository) {
        this.boothRepository = boothRepository;
        this.boothClickLogRepository = boothClickLogRepository;
    }

    @Transactional
    public void create(Long boothId) {
        if (!boothRepository.existsById(boothId)) {
            throw new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND);
        }

        boothClickLogRepository.save(BoothClickLog.create(boothId, LocalDateTime.now()));
    }
}
