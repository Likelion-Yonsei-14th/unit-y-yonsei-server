package com.likelion.yonsei.daedongje.domain.info.dto;

import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 응답")
public record NoticeResponse(
        Long id,
        String title,
        String content,
        String date,
        boolean hasImage,
        String imageUrl,
        boolean isPinned,
        String category,
        int viewCount,
        Long performanceId,
        Long boothId
) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt().toLocalDate().toString(),
                notice.hasImage(),
                notice.getImageUrl(),
                notice.isPinned(),
                notice.getCategory(),
                notice.getViewCount(),
                notice.getPerformanceId(),
                notice.getBoothId()
        );
    }
}
