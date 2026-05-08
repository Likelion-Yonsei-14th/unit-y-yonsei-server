package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.NoticeUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notice", description = "공지사항 관리 API")
@RestController
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "공지사항 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getNotices() {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotices()));
    }

    @Operation(summary = "공지사항 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(@Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(noticeService.createNotice(request)));
    }

    @Operation(summary = "공지사항 수정")
    @PutMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.updateNotice(noticeId, request)));
    }

    @Operation(summary = "공지사항 삭제")
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
}
