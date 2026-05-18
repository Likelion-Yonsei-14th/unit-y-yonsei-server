package com.likelion.yonsei.daedongje.domain.performance.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 운영진이 수동 지정하는 '현재 라이브 공연' 단일 포인터.
 *
 * <p>전역 단일 값이므로 항상 {@link #SINGLETON_ID}(= 1) 한 행만 존재한다.
 * {@code performance} 가 null 이면 라이브 미지정 상태다.
 * 라이브 포인터는 {@code Performance.performanceStatus} 와 직교하며, 상태를 바꾸지 않는다.
 */
@Getter
@Entity
@Table(name = "live_performance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LivePerformance extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    // 핀된 공연이 삭제되면 포인터를 자동으로 해제(NULL)한다. Flyway FK 의 ON DELETE SET NULL 과 일치.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Performance performance;

    private LivePerformance(Long id) {
        this.id = id;
    }

    /** 시드 행이 없을 때(예: 테스트 환경) 서비스가 새로 만들 수 있는 싱글톤 행. */
    public static LivePerformance singleton() {
        return new LivePerformance(SINGLETON_ID);
    }

    /** 라이브 공연을 지정/교체한다. {@code null} 을 넘기면 해제한다. */
    public void updatePerformance(Performance performance) {
        this.performance = performance;
    }
}
