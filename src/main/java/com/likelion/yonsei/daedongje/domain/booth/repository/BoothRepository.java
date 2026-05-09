package com.likelion.yonsei.daedongje.domain.booth.repository;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothRepository extends JpaRepository<Booth, Long> {

    boolean existsByName(String name);

    List<Booth> findAllByDate(Integer date);

    List<Booth> findAllBySector(BoothSector sector);

    List<Booth> findAllByIsFood(Boolean isFood);

    List<Booth> findAllByDateAndSector(Integer date, BoothSector sector);

    List<Booth> findAllByDateAndIsFood(Integer date, Boolean isFood);

    List<Booth> findAllBySectorAndIsFood(BoothSector sector, Boolean isFood);

    List<Booth> findAllByDateAndSectorAndIsFood(Integer date, BoothSector sector, Boolean isFood);
}
