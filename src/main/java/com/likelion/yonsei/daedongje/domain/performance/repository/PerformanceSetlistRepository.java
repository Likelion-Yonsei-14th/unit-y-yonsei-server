package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerformanceSetlistRepository extends JpaRepository<PerformanceSetlist, Long> {

    List<PerformanceSetlist> findAllByPerformanceIdOrderBySongOrderAscIdAsc(Long performanceId);

    Optional<PerformanceSetlist> findByIdAndPerformanceId(Long id, Long performanceId);

    // 공연 삭제 가드 — 해당 공연에 셋리스트가 1건이라도 있는지 확인 (BAC-110)
    boolean existsByPerformanceId(Long performanceId);
}
