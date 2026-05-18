package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceSetlistRepository extends JpaRepository<PerformanceSetlist, Long> {

    List<PerformanceSetlist> findAllByPerformanceIdOrderBySongOrderAscIdAsc(Long performanceId);
}
