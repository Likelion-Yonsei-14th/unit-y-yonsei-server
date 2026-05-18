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

    @Query("""
            SELECT p FROM Performance p
            LEFT JOIN FETCH p.location
            WHERE p.id = :id
              AND p.performanceStatus <> :status
            """)
    Optional<Performance> findByIdAndStatusNotWithLocation(
            @Param("id") Long id,
            @Param("status") PerformanceStatus status
    );

    List<Performance> findAllByPerformanceStatus(PerformanceStatus performanceStatus);

    @Query("""
            SELECT p FROM Performance p
            LEFT JOIN FETCH p.location
            WHERE p.performanceStatus = :status
            ORDER BY p.performanceDate ASC, p.startTime ASC, p.id ASC
            """)
    List<Performance> findAllByStatusWithLocation(@Param("status") PerformanceStatus status);

    List<Performance> findAllByPerformanceStatusNot(PerformanceStatus performanceStatus);

    @Query("""
            SELECT p FROM Performance p
            LEFT JOIN FETCH p.location
            WHERE p.performanceStatus <> :status
            ORDER BY p.performanceDate ASC, p.startTime ASC, p.id ASC
            """)
    List<Performance> findAllPublicWithLocation(@Param("status") PerformanceStatus status);

    boolean existsByAdminUser(AdminUser adminUser);
}
