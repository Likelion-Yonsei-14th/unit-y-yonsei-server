package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothClickLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BoothClickLogRepository extends JpaRepository<BoothClickLog, Long> {

    @Query("""
            SELECT l.boothId AS boothId, COUNT(l.id) AS clickCount
            FROM BoothClickLog l
            WHERE l.clickedAt >= :startAt
              AND l.clickedAt < :endAt
            GROUP BY l.boothId
            ORDER BY COUNT(l.id) DESC, l.boothId ASC
            """)
    List<PopularBoothSummary> findPopularBooths(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            Pageable pageable
    );
}
