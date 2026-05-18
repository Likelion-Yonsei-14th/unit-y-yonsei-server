package com.likelion.yonsei.daedongje.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data {@link Page} 를 클라이언트 친화적으로 평탄화한 응답 래퍼.
 *
 * <p>{@code Page} 를 그대로 반환하면 Spring 내부 직렬화 형태(pageable, sort 등)가 노출되어
 * 프론트 작성이 번거롭고 향후 변경 시 깨질 수 있다. 본 클래스로 일관된 페이지네이션 응답 구조를 보장한다.
 *
 * <p>사용 예:
 * <pre>{@code
 *   Page<Booth> page = boothRepository.findAll(pageable);
 *   return ApiResponse.success(PageResponse.from(page.map(BoothDto::from)));
 * }</pre>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
