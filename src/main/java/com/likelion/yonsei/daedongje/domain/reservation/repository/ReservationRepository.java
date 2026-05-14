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

    long countByBoothIdAndStatus(Long boothId, ReservationStatus status);

    // 사용자 예약 목록 조회 (이름 + 연락처 + 선택적 상태 기준)
    // PIN 일치 여부는 BCrypt 비교가 필요하므로 서비스 레이어에서 필터링
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.bookerName = :bookerName
              AND r.phoneNumber = :phoneNumber
              AND (:status IS NULL OR r.status = :status)
            """)
    List<Reservation> findAllByBookerNameAndPhoneNumberWithFilter(
            @Param("bookerName") String bookerName,
            @Param("phoneNumber") String phoneNumber,
            @Param("status") ReservationStatus status);
}
