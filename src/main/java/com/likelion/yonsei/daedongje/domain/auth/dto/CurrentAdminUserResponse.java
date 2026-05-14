package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CurrentAdminUserResponse {

    private Long adminUserId;
    private String loginId;
    private String organization;
    private String role;
    private String status;
    private String representativeName;

    public static CurrentAdminUserResponse from(AdminUser adminUser) {
        return new CurrentAdminUserResponse(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getOrganization(),
                adminUser.getRole().name(),
                adminUser.getStatus().name(),
                adminUser.getRepresentativeName()
        );
    }
}