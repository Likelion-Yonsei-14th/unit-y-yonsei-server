package com.likelion.yonsei.daedongje.domain.info.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeImageRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import com.likelion.yonsei.daedongje.domain.info.entity.NoticeImage;
import com.likelion.yonsei.daedongje.domain.info.exception.NoticeErrorCode;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public List<NoticeResponse> getNotices() {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDescIdDesc().stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.create(
                request.title(),
                request.content(),
                Boolean.TRUE.equals(request.isPinned()),
                request.category(),
                request.performanceId(),
                request.boothId()
        );
        notice.replaceImages(toNoticeImages(request.images(), request.imageUrl(), request.hasImage()));

        return NoticeResponse.from(noticeRepository.saveAndFlush(notice));
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = findNotice(noticeId);
        notice.update(
                request.title(),
                request.content(),
                request.isPinned(),
                request.category(),
                request.performanceId(),
                request.boothId()
        );
        notice.replaceImages(toNoticeImages(request.images(), request.imageUrl(), request.hasImage()));
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

    private List<NoticeImage> toNoticeImages(List<NoticeImageRequest> imageRequests, String imageUrl, Boolean hasImage) {
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

    private String resolveFallbackImageUrl(String imageUrl, Boolean hasImage) {
        if (StringUtils.hasText(imageUrl)) {
            return imageUrl.trim();
        }

        if (Boolean.TRUE.equals(hasImage)) {
            return "pending-upload";
        }

        return null;
    }
}
