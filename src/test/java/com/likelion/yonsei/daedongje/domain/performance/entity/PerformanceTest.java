package com.likelion.yonsei.daedongje.domain.performance.entity;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerformanceTest {

    @Test
    void create_uses_required_name_and_admin_only() {
        AdminUser adminUser = adminUser();

        Performance performance = Performance.create(adminUser, "Main Stage");

        assertThat(performance.getAdminUser()).isEqualTo(adminUser);
        assertThat(performance.getCreatedBy()).isEqualTo(1L);
        assertThat(performance.getPerformanceName()).isEqualTo("Main Stage");
        assertThat(performance.getPerformanceStatus()).isEqualTo(PerformanceStatus.HIDDEN);
    }

    @Test
    void create_rejects_blank_name() {
        AdminUser adminUser = adminUser();

        assertThatThrownBy(() -> Performance.create(adminUser, " "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_allows_optional_fields_to_remain_null() {
        Performance performance = Performance.create(adminUser(), "Main Stage");

        assertThat(performance.getLocation()).isNull();
        assertThat(performance.getPerformanceDescription()).isNull();
        assertThat(performance.getPerformanceDate()).isNull();
        assertThat(performance.getStartTime()).isNull();
        assertThat(performance.getEndTime()).isNull();
        assertThat(performance.getPerformanceCategory()).isNull();
        assertThat(performance.getLineupName()).isNull();
    }

    @Test
    void updateBasicInfo_changes_only_non_null_fields() {
        Performance performance = Performance.create(adminUser(), "Main Stage");

        performance.updateBasicInfo(
                null,
                "Updated Stage",
                "Updated description",
                2,
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                "BAND",
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );

        assertThat(performance.getPerformanceName()).isEqualTo("Updated Stage");
        assertThat(performance.getPerformanceDescription()).isEqualTo("Updated description");
        assertThat(performance.getPerformanceDate()).isEqualTo(2);
        assertThat(performance.getStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(performance.getEndTime()).isEqualTo(LocalTime.of(19, 30));
        assertThat(performance.getPerformanceCategory()).isEqualTo("BAND");
        assertThat(performance.getLineupName()).isEqualTo("Lineup A");
        assertThat(performance.getPerformanceStatus()).isEqualTo(PerformanceStatus.SCHEDULED);
    }

    @Test
    void updateBasicInfo_preserves_existing_values_when_null_is_passed() {
        Performance performance = Performance.create(adminUser(), "Main Stage");
        performance.updateBasicInfo(
                null,
                "Updated Stage",
                "Updated description",
                2,
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                "BAND",
                "Lineup A",
                PerformanceStatus.SCHEDULED
        );

        performance.updateBasicInfo(null, null, null, null, null, null, null, null, null);

        assertThat(performance.getPerformanceName()).isEqualTo("Updated Stage");
        assertThat(performance.getPerformanceDescription()).isEqualTo("Updated description");
        assertThat(performance.getPerformanceDate()).isEqualTo(2);
        assertThat(performance.getStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(performance.getEndTime()).isEqualTo(LocalTime.of(19, 30));
        assertThat(performance.getPerformanceCategory()).isEqualTo("BAND");
        assertThat(performance.getLineupName()).isEqualTo("Lineup A");
        assertThat(performance.getPerformanceStatus()).isEqualTo(PerformanceStatus.SCHEDULED);
    }

    private AdminUser adminUser() {
        AdminUser adminUser = AdminUser.create(
                "performer",
                "password-hash",
                "Performance Team",
                AdminRole.PERFORMER,
                "Representative",
                "010-0000-0000",
                null
        );
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        return adminUser;
    }
}
