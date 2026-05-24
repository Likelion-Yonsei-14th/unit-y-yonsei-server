package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.CheerMessageDisplayStatus;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
              AND m.displayStatus = :displayStatus
            ORDER BY m.createdAt ASC, m.id ASC
            """)
    List<PerformanceCheerMessage> findAllByPerformanceAndDisplayStatusWithRelationsOrderByCreatedAtAscIdAsc(
            @Param("performance") Performance performance,
            @Param("displayStatus") CheerMessageDisplayStatus displayStatus
    );

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
            ORDER BY m.createdAt ASC, m.id ASC
            """)
    List<PerformanceCheerMessage> findAllWithRelationsOrderByCreatedAtAscIdAsc();

    @Query("""
            SELECT m FROM PerformanceCheerMessage m
            LEFT JOIN FETCH m.setlist
            JOIN FETCH m.performance
            WHERE m.id = :id
            """)
    Optional<PerformanceCheerMessage> findByIdWithRelations(@Param("id") Long id);

    @Query(
            value = """
                    SELECT m FROM PerformanceCheerMessage m
                    LEFT JOIN FETCH m.setlist
                    WHERE m.performance = :performance
                    ORDER BY m.createdAt DESC, m.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(m) FROM PerformanceCheerMessage m
                    WHERE m.performance = :performance
                    """
    )
    Page<PerformanceCheerMessage> findPageByPerformanceOrderByCreatedAtDescIdDesc(
            @Param("performance") Performance performance,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT m FROM PerformanceCheerMessage m
                    LEFT JOIN FETCH m.setlist
                    WHERE m.performance = :performance AND m.setlist.id = :setlistId
                    ORDER BY m.createdAt DESC, m.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(m) FROM PerformanceCheerMessage m
                    WHERE m.performance = :performance AND m.setlist.id = :setlistId
                    """
    )
    Page<PerformanceCheerMessage> findPageByPerformanceAndSetlistIdOrderByCreatedAtDescIdDesc(
            @Param("performance") Performance performance,
            @Param("setlistId") Long setlistId,
            Pageable pageable
    );
}
