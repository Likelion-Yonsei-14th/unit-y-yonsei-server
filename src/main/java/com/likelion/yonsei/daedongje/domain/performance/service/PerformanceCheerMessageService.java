package com.likelion.yonsei.daedongje.domain.performance.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.response.PageResponse;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminUser;
import com.likelion.yonsei.daedongje.domain.auth.exception.AuthErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.repository.AdminUserRepository;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.performance.dto.FavoriteStageResultResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageCreateRequest;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceCheerMessageResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceReviewResponse;
import com.likelion.yonsei.daedongje.domain.performance.dto.PerformanceReviewSummaryResponse;
import com.likelion.yonsei.daedongje.domain.performance.entity.CheerMessageDisplayStatus;
import com.likelion.yonsei.daedongje.domain.performance.entity.Performance;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceCheerMessage;
import com.likelion.yonsei.daedongje.domain.performance.entity.PerformanceSetlist;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceCheerMessageErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceSetlistErrorCode;
import com.likelion.yonsei.daedongje.domain.performance.repository.FavoriteStageVoteCountProjection;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceCheerMessageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceSetlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public List<PerformanceCheerMessageResponse> getMyPerformanceCheerMessages(AdminSessionUser currentAdmin) {
        Performance performance = findMyPerformance(currentAdmin);
        return cheerMessageRepository.findAllByPerformanceWithRelationsOrderByCreatedAtAscIdAsc(performance)
                .stream()
                .map(PerformanceCheerMessageResponse::from)
                .toList();
    }

    public PerformanceReviewSummaryResponse getMyPerformanceReviewSummary(AdminSessionUser currentAdmin) {
        Performance performance = findMyPerformance(currentAdmin);
        long totalVoteCount = cheerMessageRepository.countByPerformanceAndSetlistIsNotNullAndDisplayStatus(
                performance,
                CheerMessageDisplayStatus.VISIBLE
        );

        List<FavoriteStageVoteCountProjection> voteCounts = cheerMessageRepository
                .countFavoriteStageVotesByPerformance(performance, CheerMessageDisplayStatus.VISIBLE);

        List<FavoriteStageResultResponse> results = new java.util.ArrayList<>();
        for (int i = 0; i < voteCounts.size(); i++) {
            FavoriteStageVoteCountProjection voteCount = voteCounts.get(i);
            results.add(FavoriteStageResultResponse.of(
                    i + 1,
                    voteCount.getSetlist(),
                    voteCount.getVoteCount(),
                    totalVoteCount
            ));
        }

        return new PerformanceReviewSummaryResponse(
                performance.getId(),
                performance.getPerformanceName(),
                totalVoteCount,
                results
        );
    }

    public PageResponse<PerformanceReviewResponse> getMyPerformanceReviews(
            AdminSessionUser currentAdmin,
            Long setlistId,
            Pageable pageable
    ) {
        Performance performance = findMyPerformance(currentAdmin);
        PerformanceSetlist setlist = findSetlistForPerformanceOrNull(setlistId, performance);

        Page<PerformanceCheerMessage> reviewPage = setlist == null
                ? cheerMessageRepository.findAllByPerformanceAndDisplayStatusOrderByCreatedAtDescIdDesc(
                        performance,
                        CheerMessageDisplayStatus.VISIBLE,
                        pageable
                )
                : cheerMessageRepository.findAllByPerformanceAndSetlistAndDisplayStatusOrderByCreatedAtDescIdDesc(
                        performance,
                        setlist,
                        CheerMessageDisplayStatus.VISIBLE,
                        pageable
                );

        return PageResponse.from(reviewPage.map(PerformanceReviewResponse::from));
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

    private PerformanceSetlist findSetlistForPerformanceOrNull(Long setlistId, Performance performance) {
        if (setlistId == null) {
            return null;
        }
        PerformanceSetlist setlist = performanceSetlistRepository.findById(setlistId)
                .orElseThrow(() -> new BusinessException(
                        PerformanceSetlistErrorCode.PERFORMANCE_SETLIST_NOT_FOUND
                ));
        if (!setlist.getPerformance().getId().equals(performance.getId())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }
        return setlist;
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
