package com.likelion.yonsei.daedongje.domain.image.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
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

    private static final Set<String> ALLOWED_DOMAINS = Set.of(
            "booth",
            "menu",
            "lost-item",
            "banner",
            "performance",
            "notice"
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

    public PresignedUrlCreateResponse createPresignedUrl(PresignedUrlCreateRequest request) {
        String domain = normalizeDomain(request.getDomain());
        String extension = extractExtension(request.getFileName());
        String contentType = normalizeContentType(request.getContentType());

        validateDomain(domain);
        validateExtension(extension);
        validateContentType(contentType);
        validateExtensionAndContentType(extension, contentType);

        String objectKey = createObjectKey(domain, extension);
        String uploadUrl = createUploadUrl(objectKey, contentType);
        String imageUrl = awsS3Properties.getNormalizedImageBaseUrl() + "/" + objectKey;

        return PresignedUrlCreateResponse.of(uploadUrl, objectKey, imageUrl);
    }

    private String normalizeDomain(String domain) {
        return domain.trim().toLowerCase();
    }

    private String normalizeContentType(String contentType) {
        return contentType.trim().toLowerCase();
    }

    private void validateDomain(String domain) {
        if (!ALLOWED_DOMAINS.contains(domain)) {
            throw new BusinessException(ImageErrorCode.INVALID_IMAGE_DOMAIN);
        }
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

    private String createUploadUrl(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3Properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(awsS3Properties.getPresignedUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }
}