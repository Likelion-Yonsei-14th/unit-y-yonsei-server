package com.likelion.yonsei.daedongje.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 JPA 엔티티가 상속하는 공통 베이스.
 *
 * <p>{@code createdAt}, {@code updatedAt} 두 컬럼을 자동 관리한다.
 * <ul>
 *   <li>{@code createdAt}: 엔티티 최초 저장 시 자동 채워지고 이후 변경되지 않음 (updatable = false).</li>
 *   <li>{@code updatedAt}: 매 저장(update) 마다 자동 갱신.</li>
 * </ul>
 *
 * <p>동작 조건:
 * <ul>
 *   <li>메인 애플리케이션 클래스에 {@code @EnableJpaAuditing} 이 활성화되어 있어야 한다 ({@code DaedongjeApplication}).</li>
 *   <li>엔티티는 본 클래스를 {@code extends} 한다 ({@code @MappedSuperclass} 라 별도 테이블 없음, 컬럼만 상속).</li>
 *   <li>대응 DB 컬럼은 Flyway 마이그레이션에서 {@code DATETIME(6) NOT NULL} 로 정의 (예: {@code V2__create_booth_table.sql}).</li>
 * </ul>
 *
 * <p>현 PR 시점에선 {@code createdBy}/{@code updatedBy} (수정자 추적) 와 soft delete 는
 * 의도적으로 미포함. 인증 도메인 머지 후 또는 도메인별 필요 시점에 별도 추가 예정.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
