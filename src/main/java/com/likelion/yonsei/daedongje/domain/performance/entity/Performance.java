package com.likelion.yonsei.daedongje.domain.performance.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@Table(name = "performances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false, unique = true)
    private AdminUser adminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private MapLocation location;

    @Column(name = "performance_name", nullable = false, length = 100)
    private String performanceName;

    @Column(name = "performance_description", columnDefinition = "TEXT")
    private String performanceDescription;

    @Column(name = "performance_date")
    private Integer performanceDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_category", length = 50)
    private PerformanceCategory performanceCategory;

    @Column(name = "lineup_name", length = 100)
    private String lineupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status", nullable = false, length = 20)
    private PerformanceStatus performanceStatus;

    private Performance(AdminUser adminUser, Long createdBy, String performanceName) {
        validatePerformanceAdmin(adminUser);
        validateCreatedBy(createdBy);
        validatePerformanceName(performanceName);

        this.createdBy = createdBy;
        this.adminUser = adminUser;
        this.performanceName = performanceName;
        this.performanceStatus = PerformanceStatus.HIDDEN;
    }

    public static Performance create(AdminUser adminUser, String performanceName) {
        return new Performance(adminUser, adminUser != null ? adminUser.getId() : null, performanceName);
    }

    public static Performance create(AdminUser adminUser, AdminUser createdByAdmin, String performanceName) {
        validateConnectedAdmin(createdByAdmin);
        return new Performance(adminUser, createdByAdmin.getId(), performanceName);
    }

    public void updateBasicInfo(
            MapLocation location,
            String performanceName,
            String performanceDescription,
            Integer performanceDate,
            LocalTime startTime,
            LocalTime endTime,
            PerformanceCategory performanceCategory,
            String lineupName,
            PerformanceStatus performanceStatus
    ) {
        if (location != null) {
            this.location = location;
        }
        if (performanceName != null) {
            validatePerformanceName(performanceName);
            this.performanceName = performanceName;
        }
        if (performanceDescription != null) {
            this.performanceDescription = performanceDescription;
        }
        if (performanceDate != null) {
            this.performanceDate = performanceDate;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (performanceCategory != null) {
            this.performanceCategory = performanceCategory;
        }
        if (lineupName != null) {
            this.lineupName = lineupName;
        }
        if (performanceStatus != null) {
            this.performanceStatus = performanceStatus;
        }
    }

    private static void validatePerformanceAdmin(AdminUser adminUser) {
        validateConnectedAdmin(adminUser);
        if (adminUser.getRole() != AdminRole.PERFORMER) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ADMIN_ROLE_REQUIRED);
        }
    }

    private static void validateConnectedAdmin(AdminUser adminUser) {
        if (adminUser == null || adminUser.getId() == null) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ADMIN_REQUIRED);
        }
    }

    private static void validateCreatedBy(Long createdBy) {
        if (createdBy == null) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_CREATED_BY_REQUIRED);
        }
    }

    private static void validatePerformanceName(String performanceName) {
        if (performanceName == null || performanceName.isBlank()) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NAME_REQUIRED);
        }
    }
}
