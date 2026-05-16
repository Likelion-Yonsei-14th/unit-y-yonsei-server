package com.likelion.yonsei.daedongje.domain.map.repository;

import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MapLocationRepository extends JpaRepository<MapLocation, Long> {

    @Query("""
            SELECT m
            FROM MapLocation m
            WHERE (:sector IS NULL OR m.sector = :sector)
              AND (:locationType IS NULL OR m.locationType = :locationType)
              AND (:displayStatus IS NULL OR m.displayStatus = :displayStatus)
            ORDER BY m.displayOrder ASC, m.id ASC
            """)
    Page<MapLocation> findAllByFilters(
            @Param("sector") String sector,
            @Param("locationType") MapLocationType locationType,
            @Param("displayStatus") MapDisplayStatus displayStatus,
            Pageable pageable
    );
}
