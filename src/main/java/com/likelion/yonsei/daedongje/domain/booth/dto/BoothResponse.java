package com.likelion.yonsei.daedongje.domain.booth.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;

import java.time.LocalTime;

public record BoothResponse(
        Long id,
        Long adminId,
        String name,
        String organization,
        String description,
        Integer date,
        LocalTime openTime,
        LocalTime closeTime,
        String sector,
        Integer location,
        String status,
        Boolean isFood,
        String instagram,
        Boolean isReservable,
        String account,
        Long locationId
) {
    public static BoothResponse from(Booth booth) {
        return new BoothResponse(
                booth.getId(),
                booth.getAdminId(),
                booth.getName(),
                booth.getOrganization(),
                booth.getDescription(),
                booth.getDate(),
                booth.getOpenTime(),
                booth.getCloseTime(),
                booth.getSector(),
                booth.getLocation(),
                booth.getStatus(),
                booth.getIsFood(),
                booth.getInstagram(),
                booth.getIsReservable(),
                booth.getAccount(),
                booth.getLocationId()
        );
    }
}
