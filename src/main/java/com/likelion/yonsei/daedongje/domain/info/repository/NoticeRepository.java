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

    // 부스 삭제 가드 — 해당 부스에 공지가 1건이라도 걸려 있는지 확인 (BAC-109)
    boolean existsByBoothId(Long boothId);

    // 공연 삭제 가드 — 해당 공연에 공지가 1건이라도 걸려 있는지 확인 (BAC-110)
    boolean existsByPerformanceId(Long performanceId);
}
