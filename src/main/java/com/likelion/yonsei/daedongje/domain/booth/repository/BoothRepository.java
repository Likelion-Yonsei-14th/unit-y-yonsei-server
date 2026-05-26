package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BoothRepository extends JpaRepository<Booth, Long> {

    // 예약 생성 시 부스별 순번 채번을 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booth b WHERE b.id = :id")
    Optional<Booth> findByIdWithLock(@Param("id") Long id);

    boolean existsByName(String name);

    // 부스 이름 유일성은 구역(sector) 범위로 한정한다 — 같은 이름이라도 구역이 다르면 허용 (BAC-144).
    // sector 가 null 이면 파생쿼리가 `sector = null`(항상 거짓)이 되어 미검출 → DB UNIQUE(name, sector) 의
    // NULL 허용 동작과 일치(구역 미지정 동명은 허용, 구역 지정 시점에 정합성 회복).
    boolean existsByNameAndSector(String name, BoothSector sector);

    // 수정 시 자기 자신을 제외하고 같은 (name, sector) 조합이 있는지 검사한다.
    boolean existsByNameAndSectorAndIdNot(String name, BoothSector sector, Long id);

    boolean existsByAdminId(Long adminId);

    // 계정당 부스 1개 정책 — 어드민이 소유한 부스 단건 조회
    Optional<Booth> findByAdminId(Long adminId);

    List<Booth> findAllByAdminIdIn(List<Long> adminIds);

    List<Booth> findAllByDate(Integer date);

    List<Booth> findAllBySector(BoothSector sector);

    List<Booth> findAllByIsFood(Boolean isFood);

    List<Booth> findAllByDateAndSector(Integer date, BoothSector sector);

    List<Booth> findAllByDateAndIsFood(Integer date, Boolean isFood);

    List<Booth> findAllBySectorAndIsFood(BoothSector sector, Boolean isFood);

    List<Booth> findAllByDateAndSectorAndIsFood(Integer date, BoothSector sector, Boolean isFood);

    @Query("SELECT DISTINCT b FROM Booth b WHERE " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.organization) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.representativeMenus) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT m FROM Menu m WHERE m.booth = b AND LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Booth> searchByKeyword(@Param("keyword") String keyword);
    List<Booth> findAllByIsReservable(Boolean isReservable);

    List<Booth> findAllByIsReservableAndStatusIn(Boolean isReservable, Collection<BoothStatus> statuses);

}
