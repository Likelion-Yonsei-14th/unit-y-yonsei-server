package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoothImageRepository extends JpaRepository<BoothImage, Long> {

    List<BoothImage> findAllByBoothIdOrderByDisplayOrderAsc(Long boothId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bi FROM BoothImage bi WHERE bi.id = :boothImageId")
    Optional<BoothImage> findByIdWithLock(@Param("boothImageId") Long boothImageId);

    Optional<BoothImage> findByBoothIdAndDisplayOrder(Long boothId, Integer displayOrder);

    @Query("SELECT bi FROM BoothImage bi WHERE bi.boothId IN :boothIds AND bi.displayOrder = 1")
    List<BoothImage> findThumbnailsByBoothIds(@Param("boothIds") List<Long> boothIds);

    boolean existsByBoothIdAndDisplayOrder(Long boothId, Integer displayOrder);

    // 부스 삭제 시 application-level cascade — 운영 DB 의 FK 가 ON DELETE CASCADE 가 아닌 경우에도 안전망으로 동작 (BAC-111).
    @Modifying
    @Query("DELETE FROM BoothImage bi WHERE bi.boothId = :boothId")
    int deleteByBoothId(@Param("boothId") Long boothId);
}