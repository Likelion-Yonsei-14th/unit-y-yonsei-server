package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceImageRepository extends JpaRepository<PerformanceImage, Long> {

    List<PerformanceImage> findAllByPerformanceIdOrderByImageOrderAscIdAsc(Long performanceId);
}
