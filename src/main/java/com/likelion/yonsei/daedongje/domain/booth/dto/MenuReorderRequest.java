package com.likelion.yonsei.daedongje.domain.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

// 메뉴 표시 순서 재정렬 요청 DTO
@Schema(description = "메뉴 순서 재정렬 요청")
public record MenuReorderRequest(

                @Schema(description = "원하는 순서대로 나열한 메뉴 ID 목록", example = "[12, 9, 27, 5]") @NotEmpty List<Long> menuIds) {
}
