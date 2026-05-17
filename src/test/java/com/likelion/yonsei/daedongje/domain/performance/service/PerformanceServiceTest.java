package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private PerformanceRepository performanceRepository;

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
    }

    @Test
    void createPerformanceForAdmin_rejects_blank_name() {
        assertThatThrownBy(() -> performanceService.createPerformanceForAdmin(adminUser(), " "))
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
