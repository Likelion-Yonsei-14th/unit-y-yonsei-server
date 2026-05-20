package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.info.repository.NoticeRepository;
import com.likelion.yonsei.daedongje.domain.map.entity.MapDisplayStatus;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocation;
import com.likelion.yonsei.daedongje.domain.map.entity.MapLocationType;
import com.likelion.yonsei.daedongje.domain.map.exception.MapLocationErrorCode;
import com.likelion.yonsei.daedongje.domain.map.repository.MapLocationRepository;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCreateServiceRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceStatus;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceImageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceSetlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private MapLocationRepository mapLocationRepository;

    @Mock
    private PerformanceImageRepository performanceImageRepository;

    @Mock
    private PerformanceSetlistRepository performanceSetlistRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private PerformanceService performanceService;

    @Test
    void createPerformanceForAdmin_saves_performance_linked_to_admin() {
        AdminUser adminUser = adminUser();
        when(performanceRepository.save(any(Performance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Performance saved = performanceService.createPerformanceForAdmin(adminUser, "Main Stage");

        ArgumentCaptor<Performance> captor = ArgumentCaptor.forClass(Performance.class);
        verify(performanceRepository).save(captor.capture());
        assertThat(saved).isEqualTo(captor.getValue());
        assertThat(saved.getAdminUser()).isEqualTo(adminUser);
        assertThat(saved.getPerformanceName()).isEqualTo("Main Stage");
        assertThat(saved.getLocation()).isNull();
        assertThat(saved.getPerformanceDate()).isNull();
        assertThat(saved.getStartTime()).isNull();
        assertThat(saved.getEndTime()).isNull();
        assertThat(saved.getPerformanceStatus()).isEqualTo(PerformanceStatus.HIDDEN);
    }

    @Test
    void createPerformanceForAdmin_can_store_creator_separately_from_performance_admin() {
        AdminUser adminUser = adminUser();
        AdminUser creator = adminUser("master", AdminRole.MASTER, 99L);
        when(performanceRepository.save(any(Performance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Performance saved = performanceService.createPerformanceForAdmin(adminUser, creator, "Main Stage");

        assertThat(saved.getAdminUser()).isEqualTo(adminUser);
        assertThat(saved.getCreatedBy()).isEqualTo(99L);
    }

    @Test
    void createPerformanceForAdmin_saves_default_info_when_request_has_optional_values() {
        AdminUser adminUser = adminUser();
        MapLocation location = mapLocation("노천극장", 10L);
        PerformanceCreateServiceRequest request = new PerformanceCreateServiceRequest(
                "Main Stage",
                2,
                location.getId(),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0)
        );
        when(mapLocationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(performanceRepository.save(any(Performance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Performance saved = performanceService.createPerformanceForAdmin(adminUser, request);

        assertThat(saved.getPerformanceName()).isEqualTo("Main Stage");
        assertThat(saved.getLocation()).isEqualTo(location);
        assertThat(saved.getPerformanceDate()).isEqualTo(2);
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(saved.getPerformanceStatus()).isEqualTo(PerformanceStatus.HIDDEN);
    }

    @Test
    void createPerformanceForAdmin_allows_null_location_id() {
        AdminUser adminUser = adminUser();
        PerformanceCreateServiceRequest request = new PerformanceCreateServiceRequest(
                "Main Stage",
                1,
                null,
                LocalTime.of(17, 0),
                LocalTime.of(18, 0)
        );
        when(performanceRepository.save(any(Performance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Performance saved = performanceService.createPerformanceForAdmin(adminUser, request);

        assertThat(saved.getLocation()).isNull();
        assertThat(saved.getPerformanceDate()).isEqualTo(1);
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(18, 0));
        verify(mapLocationRepository, never()).findById(any());
    }

    @Test
    void createPerformanceForAdmin_rejects_unknown_location_id() {
        AdminUser adminUser = adminUser();
        PerformanceCreateServiceRequest request = new PerformanceCreateServiceRequest(
                "Main Stage",
                null,
                999L,
                null,
                null
        );
        when(mapLocationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> performanceService.createPerformanceForAdmin(adminUser, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MapLocationErrorCode.MAP_LOCATION_NOT_FOUND));
    }

    @Test
    void createPerformanceForAdmin_rejects_blank_name() {
        assertThatThrownBy(() -> performanceService.createPerformanceForAdmin(adminUser(), " "))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_NAME_REQUIRED));
    }

    @Test
    void createPerformanceForAdmin_rejects_blank_name_from_request() {
        PerformanceCreateServiceRequest request = new PerformanceCreateServiceRequest(" ", null, null, null, null);

        assertThatThrownBy(() -> performanceService.createPerformanceForAdmin(adminUser(), request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_NAME_REQUIRED));
    }

    @Test
    void createPerformanceForAdmin_rejects_duplicate_admin_performance() {
        AdminUser adminUser = adminUser();
        when(performanceRepository.existsByAdminUser(adminUser)).thenReturn(true);

        assertThatThrownBy(() -> performanceService.createPerformanceForAdmin(adminUser, "Main Stage"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createPerformanceForAdmin_rejects_unconnected_admin() {
        AdminUser adminUser = AdminUser.create(
                "performer",
                "password-hash",
                "Performance Team",
                AdminRole.PERFORMER,
                "Representative",
                "010-0000-0000",
                null
        );

        assertThatThrownBy(() -> performanceService.createPerformanceForAdmin(adminUser, "Main Stage"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updatePerformance_applies_partial_update_when_id_exists() {
        AdminUser adminUser = adminUser();
        Performance performance = Performance.create(adminUser, adminUser, "Original Stage");
        ReflectionTestUtils.setField(performance, "id", 7L);
        when(performanceRepository.findById(7L)).thenReturn(Optional.of(performance));

        PerformanceUpdateRequest request = new PerformanceUpdateRequest(
                null, "Updated Stage", null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        performanceService.updatePerformance(7L, request);

        // updateBasicInfo 가 non-null 필드만 갱신 — performanceName 만 적용됐는지 확인
        assertThat(performance.getPerformanceName()).isEqualTo("Updated Stage");
    }

    @Test
    void updatePerformance_throws_performance_not_found_when_id_missing() {
        when(performanceRepository.findById(999L)).thenReturn(Optional.empty());

        PerformanceUpdateRequest request = new PerformanceUpdateRequest(
                null, "Updated Stage", null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> performanceService.updatePerformance(999L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    @Test
    void deletePerformance_succeeds_when_no_child_data() {
        AdminUser adminUser = adminUser();
        Performance performance = Performance.create(adminUser, adminUser, "Main Stage");
        ReflectionTestUtils.setField(performance, "id", 7L);
        when(performanceRepository.findById(7L)).thenReturn(Optional.of(performance));
        when(performanceImageRepository.existsByPerformanceId(7L)).thenReturn(false);
        when(performanceSetlistRepository.existsByPerformanceId(7L)).thenReturn(false);
        when(noticeRepository.existsByPerformanceId(7L)).thenReturn(false);

        performanceService.deletePerformance(7L);

        verify(performanceRepository).delete(performance);
    }

    @Test
    void deletePerformance_blocks_when_images_exist() {
        AdminUser adminUser = adminUser();
        Performance performance = Performance.create(adminUser, adminUser, "Main Stage");
        ReflectionTestUtils.setField(performance, "id", 7L);
        when(performanceRepository.findById(7L)).thenReturn(Optional.of(performance));
        when(performanceImageRepository.existsByPerformanceId(7L)).thenReturn(true);

        assertThatThrownBy(() -> performanceService.deletePerformance(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_HAS_IMAGES));

        verify(performanceRepository, never()).delete(any(Performance.class));
    }

    @Test
    void deletePerformance_blocks_when_setlists_exist() {
        AdminUser adminUser = adminUser();
        Performance performance = Performance.create(adminUser, adminUser, "Main Stage");
        ReflectionTestUtils.setField(performance, "id", 7L);
        when(performanceRepository.findById(7L)).thenReturn(Optional.of(performance));
        when(performanceImageRepository.existsByPerformanceId(7L)).thenReturn(false);
        when(performanceSetlistRepository.existsByPerformanceId(7L)).thenReturn(true);

        assertThatThrownBy(() -> performanceService.deletePerformance(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_HAS_SETLISTS));

        verify(performanceRepository, never()).delete(any(Performance.class));
    }

    @Test
    void deletePerformance_blocks_when_notices_exist() {
        AdminUser adminUser = adminUser();
        Performance performance = Performance.create(adminUser, adminUser, "Main Stage");
        ReflectionTestUtils.setField(performance, "id", 7L);
        when(performanceRepository.findById(7L)).thenReturn(Optional.of(performance));
        when(performanceImageRepository.existsByPerformanceId(7L)).thenReturn(false);
        when(performanceSetlistRepository.existsByPerformanceId(7L)).thenReturn(false);
        when(noticeRepository.existsByPerformanceId(7L)).thenReturn(true);

        assertThatThrownBy(() -> performanceService.deletePerformance(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_HAS_NOTICES));

        verify(performanceRepository, never()).delete(any(Performance.class));
    }

    @Test
    void deletePerformance_throws_not_found_when_id_missing() {
        when(performanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> performanceService.deletePerformance(999L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    @Test
    void deletePerformance_converts_fk_violation_to_business_exception_on_race() {
        AdminUser adminUser = adminUser();
        Performance performance = Performance.create(adminUser, adminUser, "Main Stage");
        ReflectionTestUtils.setField(performance, "id", 7L);
        when(performanceRepository.findById(7L)).thenReturn(Optional.of(performance));
        // 1차 검사 — 자식 없음. 2차 recheck — 이미지가 race 로 생김.
        when(performanceImageRepository.existsByPerformanceId(7L)).thenReturn(false, true);
        when(performanceSetlistRepository.existsByPerformanceId(7L)).thenReturn(false);
        when(noticeRepository.existsByPerformanceId(7L)).thenReturn(false);
        // 실 운영에서는 delete() 자체가 아닌 flush() 에서 FK 위반이 발생. flush() 를 throw 시키는 형태로
        // 단언해야 production 코드에서 flush() 가 빠질 경우 이 테스트가 실패한다.
        doThrow(new DataIntegrityViolationException("FK violation: fk_performance_images_performance"))
                .when(performanceRepository).flush();

        assertThatThrownBy(() -> performanceService.deletePerformance(7L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(PerformanceErrorCode.PERFORMANCE_HAS_IMAGES));

        verify(performanceRepository).flush();
    }

    private AdminUser adminUser() {
        return adminUser("performer", AdminRole.PERFORMER, 1L);
    }

    private AdminUser adminUser(String loginId, AdminRole role, Long id) {
        AdminUser adminUser = AdminUser.create(
                loginId,
                "password-hash",
                "Performance Team",
                role,
                "Representative",
                "010-0000-0000",
                null
        );
        ReflectionTestUtils.setField(adminUser, "id", id);
        return adminUser;
    }

    private MapLocation mapLocation(String locationName, Long id) {
        MapLocation location = MapLocation.create(
                locationName,
                "PERF",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ONE,
                MapLocationType.STAGE,
                1,
                MapDisplayStatus.VISIBLE
        );
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }
}
