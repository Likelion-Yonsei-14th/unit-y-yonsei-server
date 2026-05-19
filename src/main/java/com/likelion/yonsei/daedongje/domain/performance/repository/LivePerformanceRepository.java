package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.LivePerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LivePerformanceRepository extends JpaRepository<LivePerformance, Long> {

    // 라이브 공연 조회 시 performance 와 location 까지 함께 로딩해 N+1 을 피한다.
    @Query("SELECT lp FROM LivePerformance lp "
            + "LEFT JOIN FETCH lp.performance p "
            + "LEFT JOIN FETCH p.location "
            + "WHERE lp.id = :id")
    Optional<LivePerformance> findWithPerformanceById(@Param("id") Long id);
}
