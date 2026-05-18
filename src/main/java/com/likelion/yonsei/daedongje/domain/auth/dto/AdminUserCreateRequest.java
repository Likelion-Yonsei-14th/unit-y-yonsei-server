package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

//import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class AdminUserCreateRequest {

    @NotBlank(message = "로그인 아이디는 필수입니다.")
    @Size(max = 50)
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해주세요.")
    private String password;

    @NotBlank(message = "소속은 필수입니다.")
    @Size(max = 100)
    private String organization;

    @NotNull(message = "권한은 필수입니다.")
    private AdminRole role;

    @NotBlank(message = "대표자 이름은 필수입니다.")
    @Size(max = 50)
    private String representativeName;

    @NotBlank(message = "대표자 전화번호는 필수입니다.")
    @Size(max = 30)
    private String representativePhone;

    @Size(max = 500)
    private String memo;

    // ===== BOOTH 역할 전용 필드 (선택사항, 서비스에서 BOOTH 검증) =====
    @Schema(description = "부스 이름 (BOOTH 역할일 때 필수)", example = "멋사 핫도그", nullable = true)
    @Size(max = 50)
    private String boothName;

    @Schema(description = "운영 캐퍼스 (BOOTH 역할일 때 선택)", example = "송도", nullable = true)
    private BoothSector boothSector;

    @Schema(description = "부스 운영 날짜. 1~3 중 하나")
    private Integer boothOperatingDate;

    @Schema(description = "자리 메모 (BOOTH 역할일 때 선택)", example = "A 구역 2번 위치 후보", nullable = true)
    @Size(max = 500)
    private String boothLocationMemo;

    // ===== PERFORMER 역할 전용 필드  =====
    @Schema(description = "공연 이름", example = "AKARAKA 밴드", nullable = true)
    @Size(max = 100)
    private String performanceName;

//    @Schema(description = "공연 일자", example = "1", nullable = true)
//    private Integer performanceDate;
//
//    @Schema(description = "공연 장소 ID", example = "3", nullable = true)
//    private Long performanceLocationId;
//
//    @Schema(description = "공연 시작 시간", example = "18:30", nullable = true)
//    private LocalTime performanceStartTime;
//
//    @Schema(description = "공연 종료 시간", example = "19:00", nullable = true)
//    private LocalTime performanceEndTime;
}