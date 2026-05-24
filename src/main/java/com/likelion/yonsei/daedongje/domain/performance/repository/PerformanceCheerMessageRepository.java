package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.CheerMessageDisplayStatus;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceCheerMessageRepository extends JpaRepository<PerformanceCheerMessage, Long> {

    @Query("""
            SELECT m FROM PerformanceCheerMessage m
            LEFT JOIN FETCH m.setlist
            JOIN FETCH m.performance
            WHERE m.performance = :performance
            ORDER BY m.createdAt ASC, m.id ASC
            """)
    List<PerformanceCheerMessage> findAllByPerformanceWithRelationsOrderByCreatedAtAscIdAsc(
            @Param("performance") Performance performance
    );

    @Query("""
            SELECT m FROM PerformanceCheerMessage m
            LEFT JOIN FETCH m.setlist
            JOIN FETCH m.performance
            WHERE m.id = :id
            """)
    Optional<PerformanceCheerMessage> findByIdWithRelations(@Param("id") Long id);

    long countByPerformanceAndSetlistIsNotNullAndDisplayStatus(
            Performance performance,
            CheerMessageDisplayStatus displayStatus
    );

    @Query("""
            SELECT m.setlist AS setlist, COUNT(m) AS voteCount
            FROM PerformanceCheerMessage m
            WHERE m.performance = :performance
              AND m.setlist IS NOT NULL
              AND m.displayStatus = :displayStatus
            GROUP BY m.setlist
            ORDER BY COUNT(m) DESC, m.setlist.songOrder ASC, m.setlist.id ASC
            """)
    List<FavoriteStageVoteCountProjection> countFavoriteStageVotesByPerformance(
            @Param("performance") Performance performance,
            @Param("displayStatus") CheerMessageDisplayStatus displayStatus
    );

    @EntityGraph(attributePaths = {"performance", "setlist"})
    Page<PerformanceCheerMessage> findAllByPerformanceAndDisplayStatusOrderByCreatedAtDescIdDesc(
            Performance performance,
            CheerMessageDisplayStatus displayStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"performance", "setlist"})
    Page<PerformanceCheerMessage> findAllByPerformanceAndSetlistAndDisplayStatusOrderByCreatedAtDescIdDesc(
            Performance performance,
            PerformanceSetlist setlist,
            CheerMessageDisplayStatus displayStatus,
            Pageable pageable
    );
}
