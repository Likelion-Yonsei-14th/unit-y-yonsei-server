package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BoothRepositoryTest {

    @Autowired
    private BoothRepository boothRepository;

    @Test
    @DisplayName("예약 가능 목록 조회는 isReservable=true 이면서 status IN (OPEN, PREPARING) 인 부스만 반환한다 — CLOSED·비예약은 제외 (R-01)")
    void findAllByIsReservableAndStatusInReturnsOpenAndPreparingReservableBooths() {
        // 축제 시작 전 예약을 미리 받는 PREPARING 부스가 status=OPEN 단독 필터에서 누락되던 버그(BAC-141) 방지.
        Booth openReservable = boothRepository.save(booth(1L, "오픈 예약부스", true, BoothStatus.OPEN));
        Booth preparingReservable = boothRepository.save(booth(2L, "준비중 예약부스", true, BoothStatus.PREPARING));
        boothRepository.save(booth(3L, "마감 예약부스", true, BoothStatus.CLOSED));   // status 로 제외
        boothRepository.save(booth(4L, "오픈 비예약부스", false, BoothStatus.OPEN));  // isReservable 로 제외

        List<Booth> result = boothRepository.findAllByIsReservableAndStatusIn(
                true, List.of(BoothStatus.OPEN, BoothStatus.PREPARING));

        assertThat(result)
                .extracting(Booth::getId)
                .containsExactlyInAnyOrder(openReservable.getId(), preparingReservable.getId());
    }

    private Booth booth(Long adminId, String name, boolean isReservable, BoothStatus status) {
        return Booth.create(
                adminId, name, "단체", "소개",
                2, LocalTime.of(11, 0), LocalTime.of(20, 0),
                BoothSector.한글탑, 1, status,
                true, null, isReservable,
                null, null, null, false, null
        );
    }
}
