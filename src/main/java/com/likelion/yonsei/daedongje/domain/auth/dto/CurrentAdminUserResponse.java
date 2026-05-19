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
    // BOOTH 역할 어드민이 소유한 부스 ID (없거나 다른 역할이면 null)
    private Long boothId;
    // PERFORMER 역할 어드민이 소유한 공연 ID (없거나 다른 역할이면 null)
    private Long performanceTeamId;

    public static CurrentAdminUserResponse from(AdminUser adminUser, Long boothId, Long performanceTeamId) {
        return new CurrentAdminUserResponse(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getOrganization(),
                adminUser.getRole().name(),
                adminUser.getStatus().name(),
                adminUser.getRepresentativeName(),
                boothId,
                performanceTeamId
        );
    }
}