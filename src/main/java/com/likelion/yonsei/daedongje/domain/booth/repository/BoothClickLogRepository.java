package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothClickLogRepository extends JpaRepository<BoothClickLog, Long> {
}
