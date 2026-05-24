package com.likelion.yonsei.daedongje.domain.info.dto;

import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import com.likelion.yonsei.daedongje.domain.info.entity.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "공지사항 응답")
public record NoticeResponse(
        // NOTE: 의도적 중복 - id/noticeId 모두 notice.getId() 값. FE가 두 키 모두 참조해 호환 위해 유지.
        Long id,
        Long noticeId,
        String title,
        String content,
        String date,
        String time,
        String instagramUrl,
        boolean hasImage,
        String imageUrl,
        boolean isPinned,
        @Schema(description = "공지사항 카테고리", allowableValues = {"BLUERUN", "BOOTH", "PERFORMANCE", "OTHERS"})
        NoticeCategory category,
        int viewCount,
        Long performanceId,
        Long boothId,
        List<NoticeImageResponse> images
) {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static NoticeResponse from(Notice notice) {
        List<NoticeImageResponse> images = notice.getImages().stream()
                .map(NoticeImageResponse::from)
                .toList();

        return new NoticeResponse(
                notice.getId(),   // id
                notice.getId(),   // noticeId (의도적 중복, FE 호환)
                notice.getTitle(),
                notice.getContent(),
                notice.getUpdatedAt().toLocalDate().toString(),
                notice.getUpdatedAt().toLocalTime().format(TIME_FORMATTER),
                notice.getInstagramUrl(),
                !images.isEmpty(),
                notice.getPrimaryImageUrl(),
                notice.isPinned(),
                notice.getCategory(),
                notice.getViewCount(),
                notice.getPerformanceId(),
                notice.getBoothId(),
                images
        );
    }
}
