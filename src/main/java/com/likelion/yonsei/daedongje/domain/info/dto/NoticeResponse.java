package com.likelion.yonsei.daedongje.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 응답")
public record NoticeResponse(
        Long id,
        String title,
        String content,
        String date,

        @JsonProperty("has_image")
        boolean hasImage,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("is_pinned")
        boolean isPinned,

        String category,

        @JsonProperty("view_count")
        int viewCount,

        @JsonProperty("performance_id")
        Long performanceId,

        @JsonProperty("booth_id")
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
