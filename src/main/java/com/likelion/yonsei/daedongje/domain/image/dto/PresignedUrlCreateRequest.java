package com.likelion.yonsei.daedongje.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PresignedUrlCreateRequest {

    @Schema(description = "이미지 사용 도메인", example = "booth")
    @NotBlank(message = "이미지 도메인은 필수입니다.")
    private String domain;

    @Schema(description = "업로드할 원본 파일명", example = "thumbnail.png")
    @NotBlank(message = "파일명은 필수입니다.")
    private String fileName;

    @Schema(description = "이미지 Content-Type", example = "image/png")
    @NotBlank(message = "Content-Type은 필수입니다.")
    private String contentType;
}