package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceImageRepository extends JpaRepository<PerformanceImage, Long> {

    List<PerformanceImage> findAllByPerformanceIdOrderByImageOrderAscIdAsc(Long performanceId);

    // 공연 삭제 가드 — 해당 공연에 이미지가 1건이라도 있는지 확인 (BAC-110)
    boolean existsByPerformanceId(Long performanceId);
}
