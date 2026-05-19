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

    @Column(name = "hashtag1", length = 6)
    private String hashtag1;

    @Column(name = "hashtag2", length = 6)
    private String hashtag2;

    @Column(name = "hashtag3", length = 6)
    private String hashtag3;

    @Column(name = "youtube_url", length = 255)
    private String youtubeUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

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
        updateBasicInfo(
                location,
                performanceName,
                performanceDescription,
                performanceDate,
                startTime,
                endTime,
                performanceCategory,
                lineupName,
                performanceStatus,
                null,
                null,
                null,
                null,
                null
        );
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
            PerformanceStatus performanceStatus,
            String hashtag1,
            String hashtag2,
            String hashtag3,
            String youtubeUrl,
            String instagramUrl
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
        validateTimeRange();
        if (performanceCategory != null) {
            this.performanceCategory = performanceCategory;
        }
        if (lineupName != null) {
            this.lineupName = lineupName;
        }
        if (hashtag1 != null) {
            this.hashtag1 = hashtag1;
        }
        if (hashtag2 != null) {
            this.hashtag2 = hashtag2;
        }
        if (hashtag3 != null) {
            this.hashtag3 = hashtag3;
        }
        if (youtubeUrl != null) {
            this.youtubeUrl = youtubeUrl;
        }
        if (instagramUrl != null) {
            this.instagramUrl = instagramUrl;
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

    // 부분 수정(PATCH)으로 한쪽 시간만 바뀌어도 최종 상태가 유효하도록, 적용 후 시간 쌍을 검증한다.
    private void validateTimeRange() {
        if (this.startTime != null && this.endTime != null
                && !this.endTime.isAfter(this.startTime)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_INVALID_TIME_RANGE);
        }
    }
}
