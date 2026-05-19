package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothClickLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BoothClickLogRepositoryTest {

    @Autowired
    private BoothClickLogRepository boothClickLogRepository;

    @Test
    @DisplayName("오늘 범위의 클릭 로그를 클릭 수 내림차순, 부스 ID 오름차순으로 집계한다")
    void findPopularBoothsAggregatesTodayClicks() {
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 27, 0, 0);
        LocalDateTime endAt = startAt.plusDays(1);

        boothClickLogRepository.save(BoothClickLog.create(1L, startAt.plusHours(1)));
        boothClickLogRepository.save(BoothClickLog.create(1L, startAt.plusHours(2)));
        boothClickLogRepository.save(BoothClickLog.create(2L, startAt.plusHours(3)));
        boothClickLogRepository.save(BoothClickLog.create(2L, startAt.plusHours(4)));
        boothClickLogRepository.save(BoothClickLog.create(3L, startAt.plusHours(5)));
        boothClickLogRepository.save(BoothClickLog.create(4L, startAt.minusMinutes(1)));

        List<PopularBoothSummary> summaries = boothClickLogRepository.findPopularBooths(
                startAt,
                endAt,
                PageRequest.of(0, 5)
        );

        assertThat(summaries).hasSize(3);
        assertThat(summaries.get(0).getBoothId()).isEqualTo(1L);
        assertThat(summaries.get(0).getClickCount()).isEqualTo(2L);
        assertThat(summaries.get(1).getBoothId()).isEqualTo(2L);
        assertThat(summaries.get(1).getClickCount()).isEqualTo(2L);
        assertThat(summaries.get(2).getBoothId()).isEqualTo(3L);
        assertThat(summaries.get(2).getClickCount()).isEqualTo(1L);
    }
}
