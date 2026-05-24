package com.likelion.yonsei.daedongje.domain.info.dto;

import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import com.likelion.yonsei.daedongje.domain.info.entity.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공지사항 상세 응답")
public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String instagramUrl,
        boolean hasImage,
        String imageUrl,
        boolean isPinned,
        @Schema(description = "공지사항 카테고리", allowableValues = {"BLUERUN", "BOOTH", "PERFORMANCE", "OTHERS"})
        NoticeCategory category,
        int viewCount,
        Long performanceId,
        Long boothId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<NoticeImageResponse> images
) {

    public static NoticeDetailResponse from(Notice notice) {
        List<NoticeImageResponse> images = notice.getImages().stream()
                .map(NoticeImageResponse::from)
                .toList();

        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getInstagramUrl(),
                !images.isEmpty(),
                notice.getPrimaryImageUrl(),
                notice.isPinned(),
                notice.getCategory(),
                notice.getViewCount(),
                notice.getPerformanceId(),
                notice.getBoothId(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                images
        );
    }
}
