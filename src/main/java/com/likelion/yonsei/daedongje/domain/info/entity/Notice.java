package com.likelion.yonsei.daedongje.domain.info.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "notice")
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "performance_id")
    private Long performanceId;

    @Column(name = "booth_id")
    private Long boothId;

    protected Notice() {
    }

    private Notice(
            String title,
            String content,
            String imageUrl,
            boolean pinned,
            String category,
            Long performanceId,
            Long boothId
    ) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.pinned = pinned;
        this.category = category;
        this.performanceId = performanceId;
        this.boothId = boothId;
        this.viewCount = 0;
    }

    public static Notice create(
            String title,
            String content,
            String imageUrl,
            boolean pinned,
            String category,
            Long performanceId,
            Long boothId
    ) {
        return new Notice(
                title.trim(),
                content.trim(),
                normalize(imageUrl),
                pinned,
                normalize(category),
                performanceId,
                boothId
        );
    }

    public void update(
            String title,
            String content,
            String imageUrl,
            boolean pinned,
            String category,
            Long performanceId,
            Long boothId
    ) {
        this.title = title.trim();
        this.content = content.trim();
        this.imageUrl = normalize(imageUrl);
        this.pinned = pinned;
        this.category = normalize(category);
        this.performanceId = performanceId;
        this.boothId = boothId;
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

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isPinned() {
        return pinned;
    }

    public String getCategory() {
        return category;
    }

    public int getViewCount() {
        return viewCount;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public Long getBoothId() {
        return boothId;
    }
}
