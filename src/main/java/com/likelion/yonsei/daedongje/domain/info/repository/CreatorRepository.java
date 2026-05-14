package com.likelion.yonsei.daedongje.domain.info.repository;

import com.likelion.yonsei.daedongje.domain.info.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreatorRepository extends JpaRepository<Creator, Long> {

    List<Creator> findAllByOrderByDisplayOrderAscIdAsc();
}
