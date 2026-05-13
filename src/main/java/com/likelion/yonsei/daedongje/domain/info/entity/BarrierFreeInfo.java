package com.likelion.yonsei.daedongje.domain.info.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "barrier_free_infos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarrierFreeInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "guide_map_image_url", length = 255)
    private String guideMapImageUrl;

    @Column(name = "facility_type", length = 50)
    private String facilityType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "map_location_id", nullable = false)
    private Long mapLocationId;

    private BarrierFreeInfo(
            String title,
            String content,
            String guideMapImageUrl,
            String facilityType,
            Integer displayOrder,
            Long mapLocationId
    ) {
        this.title = title;
        this.content = content;
        this.guideMapImageUrl = guideMapImageUrl;
        this.facilityType = facilityType;
        this.displayOrder = displayOrder;
        this.mapLocationId = mapLocationId;
    }

    public static BarrierFreeInfo create(
            String title,
            String content,
            String guideMapImageUrl,
            String facilityType,
            Integer displayOrder,
            Long mapLocationId
    ) {
        return new BarrierFreeInfo(
                title.trim(),
                content.trim(),
                normalize(guideMapImageUrl),
                normalize(facilityType),
                displayOrder,
                mapLocationId
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
