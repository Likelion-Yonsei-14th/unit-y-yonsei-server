package com.likelion.yonsei.daedongje.domain.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminUserPasswordResetResponse {

    private Long id;
    private String temporaryPassword;

    public static AdminUserPasswordResetResponse from(Long id, String temporaryPassword) {
        return new AdminUserPasswordResetResponse(id, temporaryPassword);
    }
}
