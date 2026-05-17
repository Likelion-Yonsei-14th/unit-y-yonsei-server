package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Schema(description = "운영 날짜 (BOOTH 역할일 때 선택, 쉼표 구분)", example = "1, 2, 3", nullable = true)
    private String boothOperatingDates; // "1,2,3"

    @Schema(description = "자리 메모 (BOOTH 역할일 때 선택)", example = "A 구역 2번 위치 후보", nullable = true)
    @Size(max = 500)
    private String boothLocationMemo;
}