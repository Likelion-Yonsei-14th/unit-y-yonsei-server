package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoothImageRepository extends JpaRepository<BoothImage, Long> {

    List<BoothImage> findAllByBoothIdOrderByDisplayOrderAsc(Long boothId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bi FROM BoothImage bi WHERE bi.id = :boothImageId")
    Optional<BoothImage> findByIdWithLock(@Param("boothImageId") Long boothImageId);

    Optional<BoothImage> findByBoothIdAndDisplayOrder(Long boothId, Integer displayOrder);

    @Query("SELECT bi FROM BoothImage bi WHERE bi.boothId IN :boothIds AND bi.displayOrder = 1")
    List<BoothImage> findThumbnailsByBoothIds(@Param("boothIds") List<Long> boothIds);

    boolean existsByBoothIdAndDisplayOrder(Long boothId, Integer displayOrder);
}
