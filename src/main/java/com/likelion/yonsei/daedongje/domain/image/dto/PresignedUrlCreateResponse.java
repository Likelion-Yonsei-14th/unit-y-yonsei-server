package com.likelion.yonsei.daedongje.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PresignedUrlCreateResponse {

    @Schema(
            description = "S3 직접 업로드용 Presigned URL",
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/booth/uuid.png?..."
    )
    private String uploadUrl;

    @Schema(
            description = "S3 객체 key",
            example = "images/booth/550e8400-e29b-41d4-a716-446655440000.png"
    )
    private String objectKey;

    @Schema(
            description = "이미지 조회 URL",
            example = "https://daedongje-2026-images.s3.ap-northeast-2.amazonaws.com/images/booth/550e8400-e29b-41d4-a716-446655440000.png"
    )
    private String imageUrl;

    @Schema(
            description = "PUT 업로드 시 동봉해야 하는 Cache-Control 값. "
                    + "Presigned URL 서명에 포함되므로 클라이언트는 이 값을 그대로 PUT 헤더에 echo 해야 한다. "
                    + "(값이 1글자라도 다르면 S3가 SignatureDoesNotMatch로 거부)",
            example = "public, max-age=31536000, immutable"
    )
    private String cacheControl;

    public static PresignedUrlCreateResponse of(
            String uploadUrl,
            String objectKey,
            String imageUrl,
            String cacheControl
    ) {
        return new PresignedUrlCreateResponse(uploadUrl, objectKey, imageUrl, cacheControl);
    }
}