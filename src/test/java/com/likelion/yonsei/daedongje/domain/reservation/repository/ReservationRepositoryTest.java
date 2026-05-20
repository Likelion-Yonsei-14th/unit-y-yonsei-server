package com.likelion.yonsei.daedongje.domain.reservation.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ReservationRepository#findRecentDuplicates} 의 JPQL 동작을 검증.
 *
 * <p>서비스 단위 테스트(모킹)는 "올바른 인자로 호출하는가" 까지만 검증할 수 있다.
 * 윈도우 경계 · status=PENDING 필터 · 정렬은 실제 SQL 동작이라 {@code @DataJpaTest} 로만 확인 가능.
 *
 * <p>JPA Auditing 은 {@code DaedongjeApplication} 의 {@code @EnableJpaAuditing} 으로 활성화되어 있고,
 * {@code @DataJpaTest} 가 해당 메인 설정을 자동 탐지하므로 별도 어노테이션은 필요 없다.
 */
@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BoothRepository boothRepository;

    @Test
    @DisplayName("윈도우 내 같은 boothId+phoneNumber 의 PENDING 예약은 반환된다")
    void returnsRecentPendingDuplicate() {
        Booth booth = boothRepository.save(booth("테스트 부스"));
        Reservation reservation = reservationRepository.save(
                Reservation.create(booth, 1, "홍길동", "010-1234-5678", 2, null));

        List<Reservation> result = reservationRepository.findRecentDuplicates(
                booth.getId(), "010-1234-5678", ReservationStatus.PENDING,
                LocalDateTime.now().minusSeconds(10));

        assertThat(result).extracting(Reservation::getId).containsExactly(reservation.getId());
    }

    @Test
    @DisplayName("since 가 윈도우 밖이면(미래) 예약이 있어도 반환되지 않는다")
    void doesNotReturnWhenSinceOutsideWindow() {
        Booth booth = boothRepository.save(booth("테스트 부스"));
        reservationRepository.save(
                Reservation.create(booth, 1, "홍길동", "010-1234-5678", 2, null));

        // since 를 미래로 두어 윈도우 밖 상황 시뮬레이션 (createdAt < since 가 되어 매치되지 않음)
        List<Reservation> result = reservationRepository.findRecentDuplicates(
                booth.getId(), "010-1234-5678", ReservationStatus.PENDING,
                LocalDateTime.now().plusMinutes(1));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("CANCELLED 상태 예약은 PENDING 필터에서 제외된다")
    void doesNotReturnCancelledReservation() {
        Booth booth = boothRepository.save(booth("테스트 부스"));
        Reservation reservation = Reservation.create(booth, 1, "홍길동", "010-1234-5678", 2, null);
        reservation.cancel();
        reservationRepository.save(reservation);

        List<Reservation> result = reservationRepository.findRecentDuplicates(
                booth.getId(), "010-1234-5678", ReservationStatus.PENDING,
                LocalDateTime.now().minusSeconds(10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 부스라도 다른 전화번호는 반환되지 않는다")
    void doesNotReturnDifferentPhoneNumber() {
        Booth booth = boothRepository.save(booth("테스트 부스"));
        reservationRepository.save(
                Reservation.create(booth, 1, "홍길동", "010-1234-5678", 2, null));

        List<Reservation> result = reservationRepository.findRecentDuplicates(
                booth.getId(), "010-9999-9999", ReservationStatus.PENDING,
                LocalDateTime.now().minusSeconds(10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 전화번호라도 다른 부스의 예약은 반환되지 않는다")
    void doesNotReturnDifferentBooth() {
        Booth boothA = boothRepository.save(booth("테스트 부스 A"));
        Booth boothB = boothRepository.save(booth("테스트 부스 B"));
        reservationRepository.save(
                Reservation.create(boothA, 1, "홍길동", "010-1234-5678", 2, null));

        List<Reservation> result = reservationRepository.findRecentDuplicates(
                boothB.getId(), "010-1234-5678", ReservationStatus.PENDING,
                LocalDateTime.now().minusSeconds(10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 phone+booth 의 PENDING 예약이 여러 건이면 createdAt 내림차순으로 정렬된다")
    void ordersByCreatedAtDesc() throws InterruptedException {
        Booth booth = boothRepository.save(booth("테스트 부스"));
        Reservation older = reservationRepository.save(
                Reservation.create(booth, 1, "홍길동", "010-1234-5678", 2, null));
        Thread.sleep(20);  // createdAt 분리 보장 (DATETIME(6) 마이크로초)
        Reservation newer = reservationRepository.save(
                Reservation.create(booth, 2, "홍길동", "010-1234-5678", 2, null));

        List<Reservation> result = reservationRepository.findRecentDuplicates(
                booth.getId(), "010-1234-5678", ReservationStatus.PENDING,
                LocalDateTime.now().minusSeconds(10));

        assertThat(result).extracting(Reservation::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    private Booth booth(String name) {
        return Booth.create(
                1L, name, "조직", "소개",
                2, LocalTime.of(11, 0), LocalTime.of(20, 0),
                BoothSector.한글탑, 3, BoothStatus.OPEN,
                true, null, true, null,
                null, null, false, null);
    }
}
