package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
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
}