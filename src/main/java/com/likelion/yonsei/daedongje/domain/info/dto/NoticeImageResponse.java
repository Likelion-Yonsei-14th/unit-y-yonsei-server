package com.likelion.yonsei.daedongje.domain.info.dto;

import com.likelion.yonsei.daedongje.domain.info.entity.NoticeImage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 이미지 응답")
public record NoticeImageResponse(
        Long id,
        String imageUrl,
        Integer displayOrder,
        String createdAt
) {

    public static NoticeImageResponse from(NoticeImage noticeImage) {
        return new NoticeImageResponse(
                noticeImage.getId(),
                noticeImage.getImageUrl(),
                noticeImage.getDisplayOrder(),
                noticeImage.getCreatedAt().toString()
        );
    }
}
