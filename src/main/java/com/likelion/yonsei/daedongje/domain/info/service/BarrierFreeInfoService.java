package com.likelion.yonsei.daedongje.domain.info.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.dto.BarrierFreeInfoResponse;
import com.likelion.yonsei.daedongje.domain.info.entity.BarrierFreeInfo;
import com.likelion.yonsei.daedongje.domain.info.exception.BarrierFreeInfoErrorCode;
import com.likelion.yonsei.daedongje.domain.info.repository.BarrierFreeInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BarrierFreeInfoService {

    private final BarrierFreeInfoRepository barrierFreeInfoRepository;

    public List<BarrierFreeInfoResponse> getBarrierFreeInfos(String facilityType) {
        List<BarrierFreeInfo> barrierFreeInfos = hasText(facilityType)
                ? barrierFreeInfoRepository.findAllByFacilityTypeOrderByDisplayOrderAscIdAsc(facilityType.trim())
                : barrierFreeInfoRepository.findAllByOrderByDisplayOrderAscIdAsc();

        return barrierFreeInfos.stream()
                .map(BarrierFreeInfoResponse::from)
                .toList();
    }

    public BarrierFreeInfoResponse getBarrierFreeInfo(Long id) {
        BarrierFreeInfo barrierFreeInfo = barrierFreeInfoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BarrierFreeInfoErrorCode.BARRIER_FREE_INFO_NOT_FOUND));

        return BarrierFreeInfoResponse.from(barrierFreeInfo);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
