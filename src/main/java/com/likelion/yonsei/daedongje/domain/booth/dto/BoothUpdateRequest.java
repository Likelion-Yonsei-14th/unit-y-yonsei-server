package com.likelion.yonsei.daedongje.domain.booth.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

@Schema(description = "부스 수정 요청")
public record BoothUpdateRequest(

        @Schema(description = "부스 이름", example = "멋사 핫도그")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "운영 단체명. 생략 시 null로 저장", example = "멋쟁이사자처럼 연세대", nullable = true)
        @Size(max = 100)
        String organization,

        @Schema(description = "부스 소개", example = "맛있는 핫도그를 판매합니다.")
        String description,

        @Schema(description = "축제 일차 (1=1일차 ~ 4=4일차). 생략 시 null로 저장", example = "1", nullable = true)
        @Min(1) @Max(4)
        Integer date,

        @Schema(description = "운영 시작 시간. 생략 시 null로 저장", example = "11:00", nullable = true)
        LocalTime openTime,

        @Schema(description = "운영 종료 시간. 생략 시 null로 저장", example = "20:00", nullable = true)
        LocalTime closeTime,

        @Schema(description = "구역. 생략 시 null로 저장", example = "한글탑", allowableValues = {"한글탑", "백양로", "송도"}, nullable = true)
        BoothSector sector,

        @Schema(description = "섹터 내 부스 배치 번호 (예: 한글탑 1~20번 중 하나). 생략 시 null로 저장", example = "3", nullable = true)
        Integer location,

        @Schema(description = "운영 상태", example = "OPEN", allowableValues = {"OPEN", "CLOSED", "PREPARING"})
        @NotNull
        BoothStatus status,

        @Schema(description = "음식 부스 여부", example = "true")
        @NotNull
        Boolean isFood,

        @Schema(description = "인스타그램 계정 URL", example = "https://instagram.com/example")
        String instagram,

        @Schema(description = "예약 가능 여부", example = "true")
        @NotNull
        Boolean isReservable,

        @Schema(description = "계좌 정보", example = "카카오뱅크 1234-5678")
        String account,

        @Schema(description = "지도 위치 엔티티 ID", example = "10")
        Long locationId
) {}
