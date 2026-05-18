package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceSetlistCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceSetlistResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceSetlistUpdateRequest;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceSetlistErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceSetlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PerformanceSetlistService {

    private final PerformanceSetlistRepository performanceSetlistRepository;
    private final PerformanceRepository performanceRepository;
    private final AdminUserRepository adminUserRepository;

    @Transactional
    public PerformanceSetlistResponse createMyPerformanceSetlist(
            AdminSessionUser currentAdmin,
            PerformanceSetlistCreateRequest request
    ) {
        Performance performance = getMyPerformance(currentAdmin);
        PerformanceSetlist performanceSetlist = PerformanceSetlist.create(
                performance,
                request.songTitle(),
                request.singerName(),
                request.songOrder(),
                request.note()
        );

        return PerformanceSetlistResponse.from(performanceSetlistRepository.save(performanceSetlist));
    }

    @Transactional
    public PerformanceSetlistResponse updateMyPerformanceSetlist(
            AdminSessionUser currentAdmin,
            Long setlistId,
            PerformanceSetlistUpdateRequest request
    ) {
        PerformanceSetlist performanceSetlist = getSetlist(setlistId);
        Performance myPerformance = getMyPerformance(currentAdmin);
        validateMySetlist(performanceSetlist, myPerformance);

        performanceSetlist.update(
                request.songTitle(),
                request.singerName(),
                request.songOrder(),
                request.note()
        );
        return PerformanceSetlistResponse.from(performanceSetlist);
    }

    @Transactional
    public void deleteMyPerformanceSetlist(AdminSessionUser currentAdmin, Long setlistId) {
        PerformanceSetlist performanceSetlist = getSetlist(setlistId);
        Performance myPerformance = getMyPerformance(currentAdmin);
        validateMySetlist(performanceSetlist, myPerformance);

        performanceSetlistRepository.delete(performanceSetlist);
    }

    public List<PerformanceSetlistResponse> getPerformanceSetlists(Long performanceId) {
        List<PerformanceSetlist> setlists =
                performanceSetlistRepository.findAllByPerformanceIdOrderBySongOrderAscIdAsc(performanceId);

        // 셋리스트가 비어 있을 때만 공연 존재 여부를 추가 확인한다.
        // (셋리스트가 1건이라도 있으면 FK상 공연은 반드시 존재하므로 조회를 1회로 줄인다)
        if (setlists.isEmpty() && !performanceRepository.existsById(performanceId)) {
            throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
        }

        return setlists.stream()
                .map(PerformanceSetlistResponse::from)
                .toList();
    }

    private Performance getMyPerformance(AdminSessionUser currentAdmin) {
        AdminUser adminUser = adminUserRepository.findById(currentAdmin.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));

        return performanceRepository.findByAdminUser(adminUser)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    private PerformanceSetlist getSetlist(Long setlistId) {
        return performanceSetlistRepository.findById(setlistId)
                .orElseThrow(() -> new BusinessException(
                        PerformanceSetlistErrorCode.PERFORMANCE_SETLIST_NOT_FOUND
                ));
    }

    private void validateMySetlist(PerformanceSetlist performanceSetlist, Performance myPerformance) {
        if (!performanceSetlist.getPerformance().getId().equals(myPerformance.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }
    }
}
