package com.likelion.yonsei.daedongje.domain.info.controller;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.service.CreatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "만든이들", description = "만든이들 조회 API")
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;

    @Operation(summary = "만든이 등록")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatorResponse> createCreator(@Valid @RequestBody CreatorCreateRequest request) {
        return ApiResponse.success(creatorService.createCreator(request));
    }

    @Operation(summary = "만든이들 목록 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ApiResponse<List<CreatorResponse>> getCreators() {
        return ApiResponse.success(creatorService.getCreators());
    }

    @Operation(summary = "만든이 단건 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 만든이 정보 (I-004)")
    @GetMapping("/{id}")
    public ApiResponse<CreatorResponse> getCreator(@PathVariable Long id) {
        return ApiResponse.success(creatorService.getCreator(id));
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
    public ApiResponse<Void> deleteCreator(@PathVariable Long id) {
        creatorService.deleteCreator(id);
        return ApiResponse.successEmpty();
    }
}
