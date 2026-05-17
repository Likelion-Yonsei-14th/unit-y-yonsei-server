package com.likelion.yonsei.daedongje.domain.info.repository;

import com.likelion.yonsei.daedongje.domain.info.entity.LostItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {

    List<LostItem> findAllByOrderByCreatedAtDescIdDesc();
}
