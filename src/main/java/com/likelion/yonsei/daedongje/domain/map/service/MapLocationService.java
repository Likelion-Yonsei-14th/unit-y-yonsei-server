package com.likelion.yonsei.daedongje.domain.map.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.exception.CommonErrorCode;
import com.likelion.yonsei.daedongje.common.response.PageResponse;
import com.likelion.yonsei.daedongje.domain.map.dto.MapLocationResponse;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.exception.MapLocationErrorCode;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapLocationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MapLocationRepository mapLocationRepository;

    public PageResponse<MapLocationResponse> getList(
            String sector,
            String locationType,
            String displayStatus,
            int page,
            int size
    ) {
        validatePageRequest(page, size);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("displayOrder").nullsLast(), Sort.Order.asc("id"))
        );

        return PageResponse.from(mapLocationRepository
                .findAllByFilters(blankToNull(sector), blankToNull(locationType), blankToNull(displayStatus), pageable)
                .map(MapLocationResponse::from));
    }

    public MapLocationResponse getById(Long id) {
        MapLocation mapLocation = mapLocationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MapLocationErrorCode.MAP_LOCATION_NOT_FOUND));

        return MapLocationResponse.from(mapLocation);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "size는 1 이상 100 이하이어야 합니다.");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
