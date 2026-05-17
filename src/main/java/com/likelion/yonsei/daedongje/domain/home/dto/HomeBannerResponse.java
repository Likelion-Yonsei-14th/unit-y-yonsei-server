package com.likelion.yonsei.daedongje.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "홈 메인 배너 응답")
@Getter
@Builder
public class HomeBannerResponse {

    @Schema(description = "배너 ID", example = "1")
    private Long id;

    @Schema(description = "배너 이미지 URL", example = "https://cdn.daedongje.yonsei.ac.kr/banners/main.png")
    private String imageUrl;

    @Schema(description = "배너 클릭 시 이동할 URL", example = "https://daedongje.yonsei.ac.kr/notices")
    private String linkUrl;

    @Schema(description = "노출 순서", example = "1")
    private Integer displayOrder;
}
