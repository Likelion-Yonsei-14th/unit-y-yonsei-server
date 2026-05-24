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
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceCheerMessageRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceRepository;
import com.likelion.yonsei.daedongje.domain.performance.repository.PerformanceSetlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public PerformanceReviewSummaryResponse getMyPerformanceReviewSummary(AdminSessionUser currentAdmin) {
        Performance performance = findMyPerformance(currentAdmin);
        List<PerformanceCheerMessage> messages =
                cheerMessageRepository.findAllByPerformanceWithRelationsOrderByCreatedAtAscIdAsc(performance);

        List<PerformanceCheerMessage> votes = messages.stream()
                .filter(m -> m.getSetlist() != null)
                .toList();
        long totalVoteCount = votes.size();

        Map<Long, List<PerformanceCheerMessage>> grouped = votes.stream()
                .collect(Collectors.groupingBy(m -> m.getSetlist().getId()));

        List<FavoriteStageResultResponse> favoriteStageResults = new ArrayList<>();
        int rank = 1;
        List<Map.Entry<Long, List<PerformanceCheerMessage>>> sorted = grouped.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, List<PerformanceCheerMessage>>>comparingInt(
                        e -> e.getValue().size()).reversed()
                        .thenComparingLong(Map.Entry::getKey))
                .toList();

        for (Map.Entry<Long, List<PerformanceCheerMessage>> entry : sorted) {
            PerformanceSetlist setlist = entry.getValue().get(0).getSetlist();
            long voteCount = entry.getValue().size();
            double voteRate = totalVoteCount > 0
                    ? Math.round((double) voteCount / totalVoteCount * 1000.0) / 10.0
                    : 0.0;
            favoriteStageResults.add(new FavoriteStageResultResponse(rank++, setlist.getId(), setlist.getSongTitle(), voteCount, voteRate));
        }

        return new PerformanceReviewSummaryResponse(
                performance.getId(),
                performance.getPerformanceName(),
                totalVoteCount,
                favoriteStageResults
        );
    }

    public PageResponse<PerformanceReviewResponse> getMyPerformanceReviews(
            AdminSessionUser currentAdmin,
            int page,
            int size,
            Long setlistId
    ) {
        Performance performance = findMyPerformance(currentAdmin);
        // 잘못된 page/size(음수·0)는 PageRequest.of 에서 IllegalArgumentException → 500 이 되므로 방어한다.
        // 정렬은 아래 두 @Query 의 ORDER BY 가 담당하므로 Pageable 에 Sort 를 중복 지정하지 않는다.
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<PerformanceCheerMessage> result;
        if (setlistId != null) {
            validateSetlistBelongsToMyPerformance(setlistId, performance);
            result = cheerMessageRepository.findPageByPerformanceAndSetlistIdOrderByCreatedAtDescIdDesc(
                    performance, setlistId, pageable);
        } else {
            result = cheerMessageRepository.findPageByPerformanceOrderByCreatedAtDescIdDesc(
                    performance, pageable);
        }

        return PageResponse.from(result.map(PerformanceReviewResponse::from));
    }

    private void validateSetlistBelongsToMyPerformance(Long setlistId, Performance performance) {
        PerformanceSetlist setlist = performanceSetlistRepository.findById(setlistId)
                .orElseThrow(() -> new BusinessException(PerformanceSetlistErrorCode.PERFORMANCE_SETLIST_NOT_FOUND));
        if (!setlist.getPerformance().getId().equals(performance.getId())) {
            throw new BusinessException(PerformanceCheerMessageErrorCode.CHEER_MESSAGE_SETLIST_FORBIDDEN);
        }
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
