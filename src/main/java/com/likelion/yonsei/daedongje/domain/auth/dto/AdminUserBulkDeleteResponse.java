package com.likelion.yonsei.daedongje.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminUserBulkDeleteResponse {

    private int deletedCount;
    private int failedCount;
    private List<FailDetail> failList;

    @Getter
    @AllArgsConstructor
    public static class FailDetail {
        private Long id;
        private String loginId;
        private String reason;
    }

    public static AdminUserBulkDeleteResponse of(int deletedCount, int failedCount, List<FailDetail> failList) {
        return new AdminUserBulkDeleteResponse(deletedCount, failedCount, failList);
    }
}
