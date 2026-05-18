package com.likelion.yonsei.daedongje.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminUserListResponse {

    private Long id;
    private String loginId;
    private String organization;
    private String role;
    private String status;
    private String representativeName;
    @Schema(description = "정보 작성 완료 여부", example = "false")
    private boolean infoCompleted;
    @Schema(description = "연동된 부스 요약 목록")
    private List<LinkedBoothSummary> linkedBooths;
    @Schema(description = "연동된 공연 요약 목록")
    private List<LinkedPerformanceSummary> linkedPerformances;

    public static AdminUserListResponse from(
            AdminUser adminUser,
            boolean infoCompleted,
            List<Booth> linkedBooths,
            List<Performance> linkedPerformances
    ) {
        return new AdminUserListResponse(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getOrganization(),
                adminUser.getRole().name(),
                adminUser.getStatus().name(),
                adminUser.getRepresentativeName(),
                infoCompleted,
                linkedBooths == null ? null : linkedBooths.stream()
                        .map(LinkedBoothSummary::from)
                        .toList(),
                linkedPerformances == null ? null : linkedPerformances.stream()
                        .map(LinkedPerformanceSummary::from)
                        .toList()
        );
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LinkedBoothSummary {
        @Schema(description = "부스 ID", example = "1")
        private Long id;
        @Schema(description = "부스 이름", example = "멋사 핫도그")
        private String name;

        public static LinkedBoothSummary from(Booth booth) {
            return new LinkedBoothSummary(booth.getId(), booth.getName());
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LinkedPerformanceSummary {
        @Schema(description = "공연 ID", example = "1")
        private Long id;
        @Schema(description = "공연 이름", example = "AKARAKA 밴드")
        private String performanceName;

        public static LinkedPerformanceSummary from(Performance performance) {
            return new LinkedPerformanceSummary(performance.getId(), performance.getPerformanceName());
        }
    }
}
