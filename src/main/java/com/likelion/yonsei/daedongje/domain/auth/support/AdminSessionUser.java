package com.likelion.yonsei.daedongje.domain.auth.support;

import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import lombok.Getter;

import java.util.Arrays;

@Getter
public class AdminSessionUser {

    private final Long id;
    private final AdminRole role;
    private final String loginId;

    public AdminSessionUser(Long id, AdminRole role, String loginId) {
        this.id = id;
        this.role = role;
        this.loginId = loginId;
    }

    public static AdminSessionUser from(AdminUser adminUser) {
        return new AdminSessionUser(
                adminUser.getId(),
                adminUser.getRole(),
                adminUser.getLoginId()
        );
    }

    public boolean hasRole(AdminRole requiredRole) {
        return role == requiredRole;
    }

    public boolean hasAnyRole(AdminRole... requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }

        return Arrays.stream(requiredRoles)
                .anyMatch(requiredRole -> role == requiredRole);
    }

    public boolean isSuper() {
        return role == AdminRole.SUPER;
    }
}