package com.likelion.yonsei.daedongje.domain.image.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.support.CurrentAdmin;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.image.dto.PresignedUrlCreateRequest;
import com.likelion.yonsei.daedongje.domain.image.dto.PresignedUrlCreateResponse;
import com.likelion.yonsei.daedongje.domain.image.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Image Upload", description = "이미지 업로드용 S3 Presigned URL API")
@RestController
@RequestMapping("/api/admin/images")
@RequiredArgsConstructor
@RequireAdminRole({
        AdminRole.SUPER,
        AdminRole.MASTER,
        AdminRole.BOOTH,
        AdminRole.PERFORMER
})
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @Operation(
            summary = "이미지 업로드용 Presigned URL 발급",
            description = "프론트엔드가 S3에 직접 이미지를 업로드할 수 있도록 Presigned PUT URL을 발급합니다."
    )
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlCreateResponse>> createPresignedUrl(
            @CurrentAdmin AdminSessionUser currentAdmin,
            @Valid @RequestBody PresignedUrlCreateRequest request
    ) {
        PresignedUrlCreateResponse response =
                imageUploadService.createPresignedUrl(currentAdmin, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}