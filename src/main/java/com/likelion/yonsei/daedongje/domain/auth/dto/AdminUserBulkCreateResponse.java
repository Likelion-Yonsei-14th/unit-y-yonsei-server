package com.likelion.yonsei.daedongje.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminUserBulkCreateResponse {
    private int successCount;
    private int failCount;
    private List<SuccessDetail> successList;
    private List<FailDetail> failList;

    @Getter
    @AllArgsConstructor
    public static class SuccessDetail {
        private String loginId;
        private String password;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class FailDetail {
        private String role;
        private String name;
        private String reason;
    }

    public static AdminUserBulkCreateResponse of(int successCount, int failCount, List<SuccessDetail> successList, List<FailDetail> failList) {
        return new AdminUserBulkCreateResponse(successCount, failCount, successList, failList);
    }
}
