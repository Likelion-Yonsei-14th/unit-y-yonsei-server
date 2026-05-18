package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    Optional<Performance> findByAdminUser(AdminUser adminUser);

    Optional<Performance> findByIdAndPerformanceStatusNot(Long id, PerformanceStatus performanceStatus);

    List<Performance> findAllByPerformanceStatus(PerformanceStatus performanceStatus);

    // 목록·타임테이블 조회는 공연마다 location 을 지연 로딩하면 N+1 이 발생하므로 fetch join 으로 함께 조회한다.
    @Query("SELECT p FROM Performance p LEFT JOIN FETCH p.location WHERE p.performanceStatus <> :status")
    List<Performance> findAllWithLocationByPerformanceStatusNot(@Param("status") PerformanceStatus status);

    boolean existsByAdminUser(AdminUser adminUser);
}
