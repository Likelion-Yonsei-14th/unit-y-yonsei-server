package com.likelion.yonsei.daedongje.domain.reservation.repository;

import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 부스별 최대 예약 순번 조회 (다음 순번 채번에 사용)
    @Query("SELECT MAX(r.reservationNumber) FROM Reservation r WHERE r.booth.id = :boothId")
    Optional<Integer> findMaxReservationNumberByBoothId(@Param("boothId") Long boothId);

    List<Reservation> findAllByBoothId(Long boothId);

    List<Reservation> findAllByBoothIdAndStatus(Long boothId, ReservationStatus status);

    // 사용자 예약 목록 조회 (이름 + 연락처 기준)
    List<Reservation> findAllByBookerNameAndPhoneNumber(String bookerName, String phoneNumber);
}
