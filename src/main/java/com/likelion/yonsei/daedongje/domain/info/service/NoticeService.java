package com.likelion.yonsei.daedongje.domain.info.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeDetailResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeImageRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import com.likelion.yonsei.daedongje.domain.info.entity.NoticeCategory;
import com.likelion.yonsei.daedongje.domain.info.entity.NoticeImage;
import com.likelion.yonsei.daedongje.domain.info.exception.NoticeErrorCode;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final PerformanceRepository performanceRepository;
    private final BoothRepository boothRepository;

    public List<NoticeResponse> getNotices(NoticeCategory category) {
        List<Notice> notices = category == null
                ? noticeRepository.findAllByOrderByPinnedDescUpdatedAtDescIdDesc()
                : noticeRepository.findAllByCategoryOrderByPinnedDescUpdatedAtDescIdDesc(category);

        return notices.stream()
                .map(NoticeResponse::from)
                .toList();
    }

    public NoticeDetailResponse getNotice(Long noticeId) {
        return NoticeDetailResponse.from(findNotice(noticeId));
    }

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        validateNoticeReferences(request.performanceId(), request.boothId());
        Notice notice = Notice.create(
                request.title(),
                request.content(),
                request.instagramUrl(),
                Boolean.TRUE.equals(request.isPinned()),
                request.category(),
                request.performanceId(),
                request.boothId()
        );
        notice.replaceImages(toCreateNoticeImages(request.images(), request.imageUrl(), request.hasImage()));

        return NoticeResponse.from(noticeRepository.saveAndFlush(notice));
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        // 수정 대상 존재 여부를 먼저 확인한 뒤(404 우선) 참조 무결성을 검증한다.
        Notice notice = findNotice(noticeId);
        validateNoticeReferences(request.performanceId(), request.boothId());
        notice.update(
                request.title(),
                request.content(),
                request.instagramUrl(),
                request.isPinned(),
                request.category(),
                request.performanceId(),
                request.boothId()
        );
        notice.replaceImages(toUpdateNoticeImages(notice, request.images(), request.imageUrl(), request.hasImage()));
        return NoticeResponse.from(noticeRepository.saveAndFlush(notice));
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = findNotice(noticeId);
        noticeRepository.delete(notice);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    // 참조 무결성: 공지에 연결된 공연/부스가 실제로 존재하는지 검증한다(깨진 참조 방지).
    private void validateNoticeReferences(Long performanceId, Long boothId) {
        if (performanceId != null && !performanceRepository.existsById(performanceId)) {
            throw new BusinessException(NoticeErrorCode.NOTICE_PERFORMANCE_NOT_FOUND);
        }
        if (boothId != null && !boothRepository.existsById(boothId)) {
            throw new BusinessException(NoticeErrorCode.NOTICE_BOOTH_NOT_FOUND);
        }
    }

    private List<NoticeImage> toCreateNoticeImages(List<NoticeImageRequest> imageRequests, String imageUrl, Boolean hasImage) {
        validateDisplayOrders(imageRequests);

        if (imageRequests != null && !imageRequests.isEmpty()) {
            return imageRequests.stream()
                    .map(request -> NoticeImage.create(request.imageUrl().trim(), request.displayOrder()))
                    .toList();
        }

        List<NoticeImage> fallbackImages = new ArrayList<>();
        String resolvedFallbackImageUrl = resolveFallbackImageUrl(imageUrl, hasImage);
        if (resolvedFallbackImageUrl != null) {
            fallbackImages.add(NoticeImage.create(resolvedFallbackImageUrl, 1));
        }
        return fallbackImages;
    }

    private List<NoticeImage> toUpdateNoticeImages(
            Notice notice,
            List<NoticeImageRequest> imageRequests,
            String imageUrl,
            Boolean hasImage
    ) {
        validateDisplayOrders(imageRequests);

        if (imageRequests != null) {
            if (imageRequests.isEmpty()) {
                return List.of();
            }

            return imageRequests.stream()
                    .map(request -> NoticeImage.create(request.imageUrl().trim(), request.displayOrder()))
                    .toList();
        }

        if (StringUtils.hasText(imageUrl)) {
            return List.of(NoticeImage.create(imageUrl.trim(), 1));
        }

        if (Boolean.FALSE.equals(hasImage)) {
            return List.of();
        }

        return new ArrayList<>(notice.getImages());
    }

    private String resolveFallbackImageUrl(String imageUrl, Boolean hasImage) {
        if (StringUtils.hasText(imageUrl)) {
            return imageUrl.trim();
        }

        if (Boolean.TRUE.equals(hasImage)) {
            return "pending-upload";
        }

        return null;
    }

    private void validateDisplayOrders(List<NoticeImageRequest> imageRequests) {
        if (imageRequests == null || imageRequests.isEmpty()) {
            return;
        }

        Set<Integer> displayOrders = new HashSet<>();
        boolean hasDuplicate = imageRequests.stream()
                .map(NoticeImageRequest::displayOrder)
                .anyMatch(displayOrder -> !displayOrders.add(displayOrder));

        if (hasDuplicate) {
            throw new BusinessException(NoticeErrorCode.INVALID_NOTICE_IMAGE_DISPLAY_ORDER);
        }
    }
}
