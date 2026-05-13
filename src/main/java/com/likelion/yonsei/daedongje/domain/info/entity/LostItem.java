package com.likelion.yonsei.daedongje.domain.info.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Entity
@Table(name = "lost_items")
public class LostItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "found_location_id")
    private Long foundLocationId;

    @Column(name = "storage_location_id")
    private Long storageLocationId;

    protected LostItem() {
    }

    private LostItem(
            String name,
            String location,
            String description,
            String imageUrl,
            String status,
            Long foundLocationId,
            Long storageLocationId
    ) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.foundLocationId = foundLocationId;
        this.storageLocationId = storageLocationId;
    }

    public static LostItem create(
            String name,
            String location,
            String description,
            String imageUrl,
            String status,
            Long foundLocationId,
            Long storageLocationId
    ) {
        return new LostItem(
                name.trim(),
                location.trim(),
                normalize(description),
                normalize(imageUrl),
                resolveStatus(status),
                foundLocationId,
                storageLocationId
        );
    }

    public void update(
            String name,
            String location,
            String description,
            String imageUrl,
            String status,
            Long foundLocationId,
            Long storageLocationId
    ) {
        this.name = name.trim();
        this.location = location.trim();
        this.description = normalize(description);
        this.imageUrl = normalize(imageUrl);
        this.status = resolveStatus(status);
        this.foundLocationId = foundLocationId;
        this.storageLocationId = storageLocationId;
    }

    public boolean hasImage() {
        return StringUtils.hasText(imageUrl);
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "STORED";
        }
        return status.trim();
    }
}
