package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.Menu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 메뉴 엔티티의 DB 접근을 담당하는 Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    @Query("SELECT m FROM Menu m WHERE m.booth.id = :boothId ORDER BY m.displayOrder ASC")
    List<Menu> findMenusByBoothId(@Param("boothId") Long boothId);

    // 메뉴 수정 시 동시성 문제 방지를 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Menu m WHERE m.id = :menuId")
    Optional<Menu> findByIdWithLock(@Param("menuId") Long menuId);

    // 같은 부스 안에서 특정 displayOrder가 이미 사용 중인지 확인
    boolean existsByBoothIdAndDisplayOrder(Long boothId, Integer displayOrder);
}
