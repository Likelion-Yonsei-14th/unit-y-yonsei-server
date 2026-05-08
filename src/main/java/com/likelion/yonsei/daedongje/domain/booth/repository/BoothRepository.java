package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothRepository extends JpaRepository<Booth, Long> {

    boolean existsByName(String name);

    List<Booth> findAllByDate(Integer date);

    List<Booth> findAllBySector(String sector);

    List<Booth> findAllByDateAndSector(Integer date, String sector);

    List<Booth> findAllByIsFood(Boolean isFood);
}
