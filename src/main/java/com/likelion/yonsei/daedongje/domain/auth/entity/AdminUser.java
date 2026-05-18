package com.likelion.yonsei.daedongje.domain.auth.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "admin_users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_users_login_id",
                        columnNames = "login_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminStatus status;


    @Column(name = "representative_name", nullable = false, length = 50)
    private String representativeName;

    @Column(name = "representative_phone", nullable = false, length = 30)
    private String representativePhone;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(length = 500)
    private String memo;

    private AdminUser(
            String loginId,
            String passwordHash,
            String organization,
            AdminRole role,
            String representativeName,
            String representativePhone,
            String memo
    ) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.organization = organization;
        this.role = role;
        this.status = AdminStatus.ACTIVE;
        this.representativeName = representativeName;
        this.representativePhone = representativePhone;
        this.memo = memo;
    }

    public static AdminUser create(
            String loginId,
            String passwordHash,
            String organization,
            AdminRole role,
            String representativeName,
            String representativePhone,
            String memo
    ) {
        return new AdminUser(
                loginId,
                passwordHash,
                organization,
                role,
                representativeName,
                representativePhone,
                memo
        );
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = AdminStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = AdminStatus.INACTIVE;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}