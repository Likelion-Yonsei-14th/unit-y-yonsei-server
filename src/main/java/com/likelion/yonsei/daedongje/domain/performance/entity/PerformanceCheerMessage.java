package com.likelion.yonsei.daedongje.domain.performance.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.performance.exception.PerformanceCheerMessageErrorCode;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(name = "performance_cheer_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceCheerMessage extends BaseEntity {

    private static final int MAX_MESSAGE_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private PerformanceSetlist setlist;

    @Column(name = "message", nullable = false, length = MAX_MESSAGE_LENGTH)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", nullable = false, length = 20)
    private CheerMessageDisplayStatus displayStatus;

    private PerformanceCheerMessage(
            Performance performance,
            PerformanceSetlist setlist,
            String message
    ) {
        validatePerformance(performance);
        validateMessage(message);

        this.performance = performance;
        this.setlist = setlist;
        this.message = message;
        this.displayStatus = CheerMessageDisplayStatus.VISIBLE;
    }

    public static PerformanceCheerMessage create(
            Performance performance,
            PerformanceSetlist setlist,
            String message
    ) {
        return new PerformanceCheerMessage(performance, setlist, message);
    }

    public void hide() {
        this.displayStatus = CheerMessageDisplayStatus.HIDDEN;
    }

    private static void validatePerformance(Performance performance) {
        if (performance == null || performance.getId() == null) {
            throw new BusinessException(PerformanceCheerMessageErrorCode.CHEER_MESSAGE_PERFORMANCE_REQUIRED);
        }
    }

    private static void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException(PerformanceCheerMessageErrorCode.CHEER_MESSAGE_CONTENT_REQUIRED);
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(PerformanceCheerMessageErrorCode.CHEER_MESSAGE_CONTENT_TOO_LONG);
        }
    }

}
