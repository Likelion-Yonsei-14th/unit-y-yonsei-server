package com.likelion.yonsei.daedongje.domain.home.service;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothImageRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.PopularBoothSummary;
import com.likelion.yonsei.daedongje.domain.home.dto.HomeBannerResponse;
import com.likelion.yonsei.daedongje.domain.home.dto.HomePopularBoothResponse;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class HomeService {

    private static final int POPULAR_BOOTH_LIMIT = 5;
    private static final ZoneId FESTIVAL_ZONE = ZoneId.of("Asia/Seoul");

    private final BoothClickLogRepository boothClickLogRepository;
    private final BoothRepository boothRepository;
    private final BoothImageRepository boothImageRepository;
    private final ReservationRepository reservationRepository;

    public HomeService(BoothClickLogRepository boothClickLogRepository,
                       BoothRepository boothRepository,
                       BoothImageRepository boothImageRepository,
                       ReservationRepository reservationRepository) {
        this.boothClickLogRepository = boothClickLogRepository;
        this.boothRepository = boothRepository;
        this.boothImageRepository = boothImageRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<HomeBannerResponse> getBanners() {
        return List.of();
    }

    public List<HomePopularBoothResponse> getPopularBooths() {
        LocalDate today = LocalDate.now(FESTIVAL_ZONE);
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = LocalDateTime.now(FESTIVAL_ZONE);

        List<PopularBoothSummary> summaries = boothClickLogRepository.findPopularBooths(
                startAt,
                endAt,
                PageRequest.of(0, POPULAR_BOOTH_LIMIT)
        );
        if (summaries.isEmpty()) {
            return List.of();
        }

        List<Long> boothIds = summaries.stream()
                .map(PopularBoothSummary::getBoothId)
                .toList();
        Map<Long, Booth> boothMap = boothRepository.findAllById(boothIds).stream()
                .collect(Collectors.toMap(Booth::getId, booth -> booth));
        Map<Long, Long> waitingCountMap = reservationRepository
                .countByBoothIdsAndStatus(boothIds, ReservationStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        Map<Long, String> thumbnailMap = boothImageRepository.findThumbnailsByBoothIds(boothIds).stream()
                .collect(Collectors.toMap(
                        BoothImage::getBoothId,
                        BoothImage::getImageUrl,
                        (existing, duplicate) -> existing
                ));

        // 클릭 로그는 부스가 삭제돼도 남을 수 있으므로, 삭제된 부스를 먼저 걸러낸 뒤
        // 순위를 1부터 다시 매겨 최종 응답 순위에 공백이 생기지 않도록 한다.
        List<PopularBoothSummary> visibleSummaries = summaries.stream()
                .filter(summary -> boothMap.containsKey(summary.getBoothId()))
                .toList();

        return IntStream.range(0, visibleSummaries.size())
                .mapToObj(index -> {
                    PopularBoothSummary summary = visibleSummaries.get(index);
                    return HomePopularBoothResponse.of(
                            index + 1,
                            summary,
                            boothMap.get(summary.getBoothId()),
                            waitingCountMap.getOrDefault(summary.getBoothId(), 0L),
                            thumbnailMap.get(summary.getBoothId())
                    );
                })
                .toList();
    }
}
