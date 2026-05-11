package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoothRepository extends JpaRepository<Booth, Long> {

    // 예약 생성 시 부스별 순번 채번을 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booth b WHERE b.id = :id")
    Optional<Booth> findByIdWithLock(@Param("id") Long id);

    boolean existsByName(String name);

    List<Booth> findAllByDate(Integer date);

    List<Booth> findAllBySector(BoothSector sector);

    List<Booth> findAllByIsFood(Boolean isFood);

    List<Booth> findAllByDateAndSector(Integer date, BoothSector sector);

    List<Booth> findAllByDateAndIsFood(Integer date, Boolean isFood);

    List<Booth> findAllBySectorAndIsFood(BoothSector sector, Boolean isFood);

    List<Booth> findAllByDateAndSectorAndIsFood(Integer date, BoothSector sector, Boolean isFood);
}
