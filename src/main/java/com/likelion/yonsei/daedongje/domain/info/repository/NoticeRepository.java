package com.likelion.yonsei.daedongje.domain.info.repository;

import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @EntityGraph(attributePaths = "images")
    List<Notice> findAllByOrderByPinnedDescCreatedAtDescIdDesc();

    @Override
    @EntityGraph(attributePaths = "images")
    Optional<Notice> findById(Long id);

    boolean existsByBoothId(Long boothId);

    boolean existsByPerformanceId(Long performanceId);
}
