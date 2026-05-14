package com.likelion.yonsei.daedongje.common.festival;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 축제 일차(2~4) 계산 공통 서비스.
 * 부스·공연·후기 등 여러 도메인에서 공유한다.
 *
 * 2026년 축제 일정:
 *   date=2: 5월 27일
 *   date=3: 5월 28일
 *   date=4: 5월 29일
 *
 * 운영 기간 이전이면 2, 이후면 4로 클램핑한다.
 */
@Service
public class FestivalDayService {

    private static final LocalDate DAY_2 = LocalDate.of(2026, 5, 27);
    private static final LocalDate DAY_3 = LocalDate.of(2026, 5, 28);
    private static final LocalDate DAY_4 = LocalDate.of(2026, 5, 29);

    public int getCurrentFestivalDay() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        if (!today.isAfter(DAY_2)) return 2;
        if (!today.isAfter(DAY_3)) return 3;
        return 4;
    }
}
