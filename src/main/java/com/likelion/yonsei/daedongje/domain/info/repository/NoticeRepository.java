package com.likelion.yonsei.daedongje.domain.info.repository;

import com.likelion.yonsei.daedongje.domain.info.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByPinnedDescCreatedAtDescIdDesc();
}
