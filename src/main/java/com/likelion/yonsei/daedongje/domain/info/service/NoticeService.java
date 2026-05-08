package com.likelion.yonsei.daedongje.domain.info.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import com.likelion.yonsei.daedongje.domain.info.exception.NoticeErrorCode;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

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
                resolveImageUrl(request.imageUrl(), request.hasImage()),
                Boolean.TRUE.equals(request.isPinned()),
                request.category(),
                request.performanceId(),
                request.boothId()
        );

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = findNotice(noticeId);
        notice.update(
                request.title(),
                request.content(),
                resolveImageUrl(request.imageUrl(), request.hasImage()),
                Boolean.TRUE.equals(request.isPinned()),
                request.category(),
                request.performanceId(),
                request.boothId()
        );
        return NoticeResponse.from(notice);
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

    private String resolveImageUrl(String imageUrl, Boolean hasImage) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl;
        }

        // 프론트가 현재 has_image 플래그만 보내는 과도기 상황을 수용한다.
        if (Boolean.TRUE.equals(hasImage)) {
            return "pending-upload";
        }

        return null;
    }
}
