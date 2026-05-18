package com.likelion.yonsei.daedongje.domain.info.repository;

import com.likelion.yonsei.daedongje.domain.info.entity.BarrierFreeInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BarrierFreeInfoRepository extends JpaRepository<BarrierFreeInfo, Long> {

    List<BarrierFreeInfo> findAllByOrderByDisplayOrderAscIdAsc();

    List<BarrierFreeInfo> findAllByFacilityTypeOrderByDisplayOrderAscIdAsc(String facilityType);
}
