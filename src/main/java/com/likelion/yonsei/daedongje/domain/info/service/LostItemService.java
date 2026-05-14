package com.likelion.yonsei.daedongje.domain.info.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.LostItemUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.entity.LostItem;
import com.likelion.yonsei.daedongje.domain.info.exception.LostItemErrorCode;
import com.likelion.yonsei.daedongje.domain.info.repository.LostItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LostItemService {

    private final LostItemRepository lostItemRepository;

    public List<LostItemResponse> getLostItems() {
        return lostItemRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(LostItemResponse::from)
                .toList();
    }

    @Transactional
    public LostItemResponse createLostItem(LostItemCreateRequest request) {
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
        LostItem lostItem = findLostItem(lostItemId);
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
