package com.likelion.yonsei.daedongje.domain.performance.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
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

@Getter
@Entity
@Table(name = "performance_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "image_order", nullable = false)
    private Integer imageOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private PerformanceImageType imageType;

    private PerformanceImage(
            Performance performance,
            String imageUrl,
            Integer imageOrder,
            PerformanceImageType imageType
    ) {
        this.performance = performance;
        this.imageUrl = imageUrl;
        this.imageOrder = imageOrder;
        this.imageType = imageType;
    }

    public static PerformanceImage create(
            Performance performance,
            String imageUrl,
            Integer imageOrder,
            PerformanceImageType imageType
    ) {
        return new PerformanceImage(performance, imageUrl, imageOrder, imageType);
    }
}
