package com.likelion.yonsei.daedongje.domain.info.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.entity.LostItem;
import com.likelion.yonsei.daedongje.domain.info.exception.LostItemErrorCode;
import com.likelion.yonsei.daedongje.domain.info.repository.LostItemRepository;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LostItemService {

    private final LostItemRepository lostItemRepository;
    private final MapLocationRepository mapLocationRepository;

    public List<LostItemResponse> getLostItems() {
        return lostItemRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(LostItemResponse::from)
                .toList();
    }

    @Transactional
    public LostItemResponse createLostItem(LostItemCreateRequest request) {
        validateLocationReferences(request.foundLocationId(), request.storageLocationId());
        // status 누락 시 STORED 기본값은 LostItem 엔티티가 일괄 처리한다(서비스 중복 제거).
        LostItem lostItem = LostItem.create(
                request.name(),
                request.location(),
                request.description(),
                resolveCreateImageUrl(request.imageUrl(), request.hasImage()),
                request.status(),
                request.foundLocationId(),
                request.storageLocationId()
        );
        return LostItemResponse.from(lostItemRepository.save(lostItem));
    }

    @Transactional
    public LostItemResponse updateLostItem(Long lostItemId, LostItemUpdateRequest request) {
        // 수정 대상 존재 여부를 먼저 확인한 뒤(404 우선) 참조 무결성을 검증한다.
        LostItem lostItem = findLostItem(lostItemId);
        validateLocationReferences(request.foundLocationId(), request.storageLocationId());
        lostItem.update(
                request.name(),
                request.location(),
                request.description(),
                resolveUpdateImageUrl(lostItem, request.imageUrl(), request.hasImage()),
                request.status(),
                request.foundLocationId(),
                request.storageLocationId()
        );
        return LostItemResponse.from(lostItem);
    }

    // 참조 무결성: 지정된 발견/보관 위치가 실제 MapLocation 으로 존재하는지 검증한다(깨진 참조 방지).
    private void validateLocationReferences(Long foundLocationId, Long storageLocationId) {
        if (foundLocationId != null && !mapLocationRepository.existsById(foundLocationId)) {
            throw new BusinessException(LostItemErrorCode.LOST_ITEM_LOCATION_NOT_FOUND);
        }
        if (storageLocationId != null && !mapLocationRepository.existsById(storageLocationId)) {
            throw new BusinessException(LostItemErrorCode.LOST_ITEM_LOCATION_NOT_FOUND);
        }
    }

    @Transactional
    public void deleteLostItem(Long lostItemId) {
        LostItem lostItem = findLostItem(lostItemId);
        lostItemRepository.delete(lostItem);
    }

    private LostItem findLostItem(Long lostItemId) {
        return lostItemRepository.findById(lostItemId)
                .orElseThrow(() -> new BusinessException(LostItemErrorCode.LOST_ITEM_NOT_FOUND));
    }

    private String resolveCreateImageUrl(String imageUrl, Boolean hasImage) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl.trim();
        }
        if (Boolean.TRUE.equals(hasImage)) {
            return "pending-upload";
        }
        return null;
    }

    private String resolveUpdateImageUrl(LostItem lostItem, String imageUrl, Boolean hasImage) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl.trim();
        }
        if (Boolean.FALSE.equals(hasImage)) {
            return null;
        }
        if (Boolean.TRUE.equals(hasImage)) {
            return lostItem.getImageUrl();
        }
        return lostItem.getImageUrl();
    }
}
