package com.likelion.yonsei.daedongje.domain.performance.repository;

import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;

public interface FavoriteStageVoteCountProjection {

    PerformanceSetlist getSetlist();

    long getVoteCount();
}
