package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCategory;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    Optional<Performance> findByAdminUser(AdminUser adminUser);

    List<Performance> findAllByAdminUser_IdIn(List<Long> adminIds);

    Optional<Performance> findByIdAndPerformanceStatusNot(Long id, PerformanceStatus performanceStatus);

    List<Performance> findAllByPerformanceStatus(PerformanceStatus performanceStatus);


    // 목록·타임테이블 조회는 공연마다 location 을 지연 로딩하면 N+1 이 발생하므로 fetch join 으로 함께 조회한다.
    @Query("SELECT p FROM Performance p LEFT JOIN FETCH p.location WHERE p.performanceStatus <> :status")
    List<Performance> findAllWithLocationByPerformanceStatusNot(@Param("status") PerformanceStatus status);

    //PerformanceStatus 조건 없이 모든 공연과 location 을 함께 조회하는 메서드 (관리자 페이지에서 사용)
    @Query("SELECT p FROM Performance p LEFT JOIN FETCH p.location")
    List<Performance> findAllWithLocation();

    // 무대별 라이브 자동 판정용. 카테고리·일차·제외상태로 1차 필터하고, 시간 윈도우 판정은 서비스에서 한다.
    // location 이 없는 공연은 무대에 귀속할 수 없으므로 inner JOIN FETCH 로 제외한다.
    @Query("SELECT p FROM Performance p JOIN FETCH p.location "
            + "WHERE p.performanceCategory = :category "
            + "AND p.performanceDate = :day "
            + "AND p.performanceStatus NOT IN :excludedStatuses")
    List<Performance> findLiveCandidatesByCategoryAndDay(
            @Param("category") PerformanceCategory category,
            @Param("day") Integer day,
            @Param("excludedStatuses") Collection<PerformanceStatus> excludedStatuses);

    boolean existsByAdminUser(AdminUser adminUser);

    // 공연 이미지 동시 등록(더블 클릭·재시도) 시 검증-저장 사이 race 를 막기 위한 쓰기 락 조회(P-A-03).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Performance p WHERE p.id = :id")
    Optional<Performance> findByIdForUpdate(@Param("id") Long id);
}
