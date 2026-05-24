package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.CheerMessageDisplayStatus;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceCheerMessageErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceSetlistErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceCheerMessageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceSetlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceCheerMessageService {

    private final PerformanceCheerMessageRepository cheerMessageRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceSetlistRepository performanceSetlistRepository;
    private final AdminUserRepository adminUserRepository;

    @Transactional
    public PerformanceCheerMessageResponse createCheerMessage(
            Long performanceId,
            PerformanceCheerMessageCreateRequest request
    ) {
        Performance performance = findPerformance(performanceId);
        PerformanceSetlist setlist = findSetlistOrNull(request.setlistId());
        validateSetlistBelongsToPerformance(setlist, performance);

        PerformanceCheerMessage cheerMessage = PerformanceCheerMessage.create(
                performance,
                setlist,
                request.message()
        );

        return PerformanceCheerMessageResponse.from(cheerMessageRepository.save(cheerMessage));
    }

    public List<PerformanceCheerMessageResponse> getVisibleCheerMessages(Long performanceId) {
        Performance performance = findPerformance(performanceId);
        return cheerMessageRepository
                .findAllByPerformanceAndDisplayStatusWithRelationsOrderByCreatedAtAscIdAsc(
                        performance,
                        CheerMessageDisplayStatus.VISIBLE
                )
                .stream()
                .map(PerformanceCheerMessageResponse::from)
                .toList();
    }

    public List<PerformanceCheerMessageResponse> getMyPerformanceCheerMessages(AdminSessionUser currentAdmin) {
        Performance performance = findMyPerformance(currentAdmin);
        return cheerMessageRepository.findAllByPerformanceWithRelationsOrderByCreatedAtAscIdAsc(performance)
                .stream()
                .map(PerformanceCheerMessageResponse::from)
                .toList();
    }

    /** SUPER/MASTER 가 전 공연의 응원 메시지를 전 상태(VISIBLE/HIDDEN) 기준으로 조회한다. */
    public List<PerformanceCheerMessageResponse> getAllCheerMessages() {
        return cheerMessageRepository.findAllWithRelationsOrderByCreatedAtAscIdAsc()
                .stream()
                .map(PerformanceCheerMessageResponse::from)
                .toList();
    }

    @Transactional
    public void hideMyPerformanceCheerMessage(AdminSessionUser currentAdmin, Long messageId) {
        Performance performance = findMyPerformance(currentAdmin);
        PerformanceCheerMessage cheerMessage = cheerMessageRepository.findByIdWithRelations(messageId)
                .orElseThrow(() -> new BusinessException(
                        PerformanceCheerMessageErrorCode.CHEER_MESSAGE_NOT_FOUND
                ));
        validateCheerMessageBelongsToPerformance(cheerMessage, performance);

        cheerMessage.hide();
    }

    private Performance findPerformance(Long performanceId) {
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    private PerformanceSetlist findSetlistOrNull(Long setlistId) {
        if (setlistId == null) {
            return null;
        }
        return performanceSetlistRepository.findById(setlistId)
                .orElseThrow(() -> new BusinessException(
                        PerformanceSetlistErrorCode.PERFORMANCE_SETLIST_NOT_FOUND
                ));
    }

    private Performance findMyPerformance(AdminSessionUser currentAdmin) {
        AdminUser adminUser = findPerformanceAdmin(currentAdmin);
        return performanceRepository.findByAdminUser(adminUser)
                .orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    private AdminUser findPerformanceAdmin(AdminSessionUser currentAdmin) {
        if (currentAdmin == null) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }
        if (currentAdmin.getRole() != AdminRole.PERFORMER && currentAdmin.getRole() != AdminRole.SUPER) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        return adminUserRepository.findById(currentAdmin.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.ADMIN_USER_NOT_FOUND));
    }

    private void validateSetlistBelongsToPerformance(PerformanceSetlist setlist, Performance performance) {
        if (setlist != null && !setlist.getPerformance().getId().equals(performance.getId())) {
            throw new BusinessException(PerformanceCheerMessageErrorCode.CHEER_MESSAGE_SETLIST_FORBIDDEN);
        }
    }

    private void validateCheerMessageBelongsToPerformance(
            PerformanceCheerMessage cheerMessage,
            Performance performance
    ) {
        if (!cheerMessage.getPerformance().getId().equals(performance.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }
    }
}
