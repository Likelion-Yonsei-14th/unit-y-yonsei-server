package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminStatus;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminUserCreateResponse {

    private Long id;
    private String loginId;
    private String organization;
    private AdminRole role;
    private AdminStatus status;
    private String representativeName;
    private String representativePhone;
    private String memo;

    public static AdminUserCreateResponse from(AdminUser adminUser) {
        return new AdminUserCreateResponse(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getOrganization(),
                adminUser.getRole(),
                adminUser.getStatus(),
                adminUser.getRepresentativeName(),
                adminUser.getRepresentativePhone(),
                adminUser.getMemo()
        );
    }
}