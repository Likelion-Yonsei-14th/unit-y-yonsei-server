package com.likelion.yonsei.daedongje.domain.home.service;

import com.likelion.yonsei.daedongje.domain.home.dto.HomeBannerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HomeService {

    public List<HomeBannerResponse> getBanners() {
        return List.of();
    }
}
