package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminUserDetailResponse {

    private Long id;
    private String loginId;
    private String organization;
    private String role;
    private String status;
    private String representativeName;
    private String representativePhone;
    private String memo;
//    private boolean infoCompleted;

    public static AdminUserDetailResponse fromDefault(AdminUser adminUser) {
        return new AdminUserDetailResponse(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getOrganization(),
                adminUser.getRole().name(),
                adminUser.getStatus().name(),
                adminUser.getRepresentativeName(),
                adminUser.getRepresentativePhone(),
                adminUser.getMemo()
//                infoCompleted
        );
    }
}
