package com.likelion.yonsei.daedongje.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BaseEntity} 의 구조(annotation, 필드 타입)를 검증하는 회귀 테스트.
 *
 * <p>Auditing 의 실제 동작은 Spring Data JPA 라이브러리가 보장하므로 실제 persistence 테스트
 * 대신 본 클래스가 올바르게 와이어링되는지만 가벼운 reflection 으로 검증한다.
 * 도메인 엔티티 PR 들에서 실제 저장-갱신 시나리오를 자연스럽게 커버할 예정.
 */
class BaseEntityTest {

    @Test
    void abstract_MappedSuperclass_로_선언되어_있다() {
        assertThat(Modifier.isAbstract(BaseEntity.class.getModifiers())).isTrue();
        assertThat(BaseEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();
    }

    @Test
    void AuditingEntityListener_가_등록되어_있다() {
        EntityListeners listeners = BaseEntity.class.getAnnotation(EntityListeners.class);
        assertThat(listeners).isNotNull();
        assertThat(listeners.value()).contains(AuditingEntityListener.class);
    }

    @Test
    void createdAt_필드는_CreatedDate_와_updatable_false_로_선언된다() throws Exception {
        Field field = BaseEntity.class.getDeclaredField("createdAt");

        assertThat(field.getType()).isEqualTo(LocalDateTime.class);
        assertThat(field.isAnnotationPresent(CreatedDate.class)).isTrue();

        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.updatable()).isFalse();
        assertThat(column.nullable()).isFalse();
        assertThat(column.name()).isEqualTo("created_at");
    }

    @Test
    void updatedAt_필드는_LastModifiedDate_와_NOT_NULL_로_선언된다() throws Exception {
        Field field = BaseEntity.class.getDeclaredField("updatedAt");

        assertThat(field.getType()).isEqualTo(LocalDateTime.class);
        assertThat(field.isAnnotationPresent(LastModifiedDate.class)).isTrue();

        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isFalse();
        assertThat(column.name()).isEqualTo("updated_at");
    }
}
