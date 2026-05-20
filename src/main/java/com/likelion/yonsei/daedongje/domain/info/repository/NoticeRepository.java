package com.likelion.yonsei.daedongje.domain.info.repository;

import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByPinnedDescCreatedAtDescIdDesc();

    // 부스 삭제 가드 — 해당 부스에 공지가 1건이라도 걸려 있는지 확인 (BAC-109)
    boolean existsByBoothId(Long boothId);
}
