package com.likelion.yonsei.daedongje.domain.reservation.repository;

import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 부스별 최대 예약 순번 조회 (다음 순번 채번에 사용)
    @Query("SELECT MAX(r.reservationNumber) FROM Reservation r WHERE r.booth.id = :boothId")
    Optional<Integer> findMaxReservationNumberByBoothId(@Param("boothId") Long boothId);

    List<Reservation> findAllByBoothId(Long boothId);

    List<Reservation> findAllByBoothIdAndStatus(Long boothId, ReservationStatus status);

    long countByBoothIdAndStatus(Long boothId, ReservationStatus status);

    // 부스 삭제 가드 — 해당 부스에 예약이 1건이라도 있는지 확인 (BAC-109)
    boolean existsByBoothId(Long boothId);

    long countByBoothIdAndStatusAndReservationNumberLessThan(Long boothId, ReservationStatus status, Integer reservationNumber);

    // 여러 부스의 PENDING 예약 수를 한 번에 조회 (N+1 방지)
    @Query("SELECT r.booth.id, COUNT(r) FROM Reservation r WHERE r.booth.id IN :boothIds AND r.status = :status GROUP BY r.booth.id")
    List<Object[]> countByBoothIdsAndStatus(@Param("boothIds") List<Long> boothIds, @Param("status") ReservationStatus status);

    // 여러 부스의 예약 수를 상태별로 한 번에 집계 (예약 현황 요약)
    // 반환 행: [부스 ID, 예약 상태, 건수] — 예약이 0건인 부스/상태는 행으로 나오지 않음
    @Query("SELECT r.booth.id, r.status, COUNT(r) FROM Reservation r WHERE r.booth.id IN :boothIds GROUP BY r.booth.id, r.status")
    List<Object[]> countGroupedByBoothIdAndStatus(@Param("boothIds") List<Long> boothIds);

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

    // 광클 멱등 처리: 같은 부스·전화번호로 since 이후 생성된 PENDING 예약 중 가장 최근 1건을 조회.
    // Spring Data 파생 쿼리로 LIMIT 1·정렬·조건을 모두 표현 — 별도 @Query 불필요. 메서드 이름이 길지만 표준 idiom.
    Optional<Reservation> findFirstByBooth_IdAndPhoneNumberAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long boothId, String phoneNumber, ReservationStatus status, LocalDateTime since);
}
