package com.likelion.yonsei.daedongje.domain.booth.dto;

import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record BoothCreateRequest(

        @NotNull
        Long adminId,

        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Size(max = 100)
        String organization,

        String description,

        @NotNull
        @Min(1) @Max(4)
        Integer date,

        @NotNull
        LocalTime openTime,

        @NotNull
        LocalTime closeTime,

        @NotBlank
        @Size(max = 10)
        String sector,

        @NotNull
        Integer location,

        @NotBlank
        @Size(max = 10)
        String status,

        @NotNull
        Boolean isFood,

        String instagram,

        @NotNull
        Boolean isReservable,

        String account,

        Long locationId
) {}
