package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.RequireAdminRole;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.service.CreatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "만든이들 관리자", description = "만든이들 CRUD 관리자 API")
@RestController
@RequestMapping("/api/admin/creators")
@RequireAdminRole({AdminRole.SUPER, AdminRole.MASTER})
@RequiredArgsConstructor
public class CreatorAdminController {

    private final CreatorService creatorService;

    @Operation(summary = "만든이 등록")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatorResponse> createCreator(@Valid @RequestBody CreatorCreateRequest request) {
        return ApiResponse.success(creatorService.createCreator(request));
    }

    @Operation(summary = "만든이 수정")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 만든이 정보 (I-004)")
    @PutMapping("/{id}")
    public ApiResponse<CreatorResponse> updateCreator(
            @PathVariable Long id,
            @Valid @RequestBody CreatorUpdateRequest request
    ) {
        return ApiResponse.success(creatorService.updateCreator(id, request));
    }

    @Operation(summary = "만든이 삭제")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 만든이 정보 (I-004)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCreator(@PathVariable Long id) {
        creatorService.deleteCreator(id);
    }
}
