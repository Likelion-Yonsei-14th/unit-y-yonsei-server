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
