package com.likelion.yonsei.daedongje.domain.image.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.image.config.AwsS3Properties;
import com.likelion.yonsei.daedongje.domain.image.dto.PresignedUrlCreateRequest;
import com.likelion.yonsei.daedongje.domain.image.dto.PresignedUrlCreateResponse;
import com.likelion.yonsei.daedongje.domain.image.exception.ImageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageUploadService {

    private static final String IMAGE_ROOT_PATH = "images";
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    // NOTE: 의도적 고정 - 객체 키가 images/<domain>/<UUID>.<ext>로 덮어쓰기 없는 불변 URL이라
    //       immutable + 1년 캐시가 안전하다(재방문 재다운로드 제거 → egress 절감).
    //       이 값은 Presigned URL 서명에 포함되므로(아래 .cacheControl 참고),
    //       응답으로 내려보내 클라이언트가 PUT 헤더에 그대로 echo 하도록 한다.
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private static final Map<String, Set<AdminRole>> ALLOWED_ROLES_BY_DOMAIN = Map.of(
            "banner", Set.of(AdminRole.SUPER, AdminRole.MASTER),
            "notice", Set.of(AdminRole.SUPER, AdminRole.MASTER),
            "lost-item", Set.of(AdminRole.SUPER, AdminRole.MASTER),
            "booth", Set.of(AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH),
            "menu", Set.of(AdminRole.SUPER, AdminRole.MASTER, AdminRole.BOOTH),
            "performance", Set.of(AdminRole.SUPER, AdminRole.MASTER, AdminRole.PERFORMER)
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    private final S3Presigner s3Presigner;
    private final AwsS3Properties awsS3Properties;

    public PresignedUrlCreateResponse createPresignedUrl(
            AdminSessionUser currentAdmin,
            PresignedUrlCreateRequest request
    ) {
        String domain = normalizeDomain(request.getDomain());
        String extension = extractExtension(request.getFileName());
        String contentType = normalizeContentType(request.getContentType());
        Long fileSize = request.getFileSize();

        validateDomain(domain);
        validateDomainAccess(domain, currentAdmin.getRole());
        validateExtension(extension);
        validateContentType(contentType);
        validateExtensionAndContentType(extension, contentType);
        validateFileSize(fileSize);

        String objectKey = createObjectKey(domain, extension);
        String uploadUrl = createUploadUrl(objectKey, contentType, fileSize);
        String imageUrl = awsS3Properties.getNormalizedImageBaseUrl() + "/" + objectKey;

        return PresignedUrlCreateResponse.of(uploadUrl, objectKey, imageUrl, CACHE_CONTROL);
    }

    private String normalizeDomain(String domain) {
        return domain.trim().toLowerCase();
    }

    private String normalizeContentType(String contentType) {
        return contentType.trim().toLowerCase();
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            throw new BusinessException(ImageErrorCode.INVALID_IMAGE_EXTENSION);
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private void validateExtension(String extension) {
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ImageErrorCode.INVALID_IMAGE_EXTENSION);
        }
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }
    }

    private void validateExtensionAndContentType(String extension, String contentType) {
        String expectedContentType = CONTENT_TYPE_BY_EXTENSION.get(extension);

        if (!expectedContentType.equals(contentType)) {
            throw new BusinessException(ImageErrorCode.IMAGE_EXTENSION_CONTENT_TYPE_MISMATCH);
        }
    }

    private String createObjectKey(String domain, String extension) {
        return IMAGE_ROOT_PATH + "/" + domain + "/" + UUID.randomUUID() + "." + extension;
    }

    private String createUploadUrl(String objectKey, String contentType, Long fileSize) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3Properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(fileSize)
                .cacheControl(CACHE_CONTROL) // 서명에 포함됨 → 클라이언트가 동일 값을 PUT 헤더로 보내야 SignatureMatch
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(awsS3Properties.getPresignedUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }

    private void validateFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(ImageErrorCode.INVALID_IMAGE_SIZE);
        }

        if (fileSize > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(ImageErrorCode.IMAGE_SIZE_EXCEEDED);
        }
    }
    private void validateDomain(String domain) {
        if (!ALLOWED_ROLES_BY_DOMAIN.containsKey(domain)) {
            throw new BusinessException(ImageErrorCode.INVALID_IMAGE_DOMAIN);
        }
    }

    private void validateDomainAccess(String domain, AdminRole role) {
        Set<AdminRole> allowedRoles = ALLOWED_ROLES_BY_DOMAIN.get(domain);

        if (!allowedRoles.contains(role)) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }
    }
}