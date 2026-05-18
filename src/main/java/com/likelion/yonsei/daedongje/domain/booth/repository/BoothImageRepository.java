package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoothImageRepository extends JpaRepository<BoothImage, Long> {

    Optional<BoothImage> findByBoothIdAndDisplayOrder(Long boothId, Integer displayOrder);

    @Query("SELECT bi FROM BoothImage bi WHERE bi.boothId IN :boothIds AND bi.displayOrder = 1")
    List<BoothImage> findThumbnailsByBoothIds(@Param("boothIds") List<Long> boothIds);
}
