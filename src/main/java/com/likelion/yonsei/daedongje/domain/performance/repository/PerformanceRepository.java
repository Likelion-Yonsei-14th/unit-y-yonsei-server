package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    Optional<Performance> findByAdminUser(AdminUser adminUser);

    Optional<Performance> findByIdAndPerformanceStatusNot(Long id, PerformanceStatus performanceStatus);

    List<Performance> findAllByPerformanceStatus(PerformanceStatus performanceStatus);

    List<Performance> findAllByPerformanceStatusNot(PerformanceStatus performanceStatus);

    boolean existsByAdminUser(AdminUser adminUser);
}
