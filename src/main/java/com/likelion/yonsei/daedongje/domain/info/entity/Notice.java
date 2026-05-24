package com.likelion.yonsei.daedongje.domain.info.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

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

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private final List<NoticeImage> images = new ArrayList<>();

    protected Notice() {
    }

    private Notice(
            String title,
            String content,
            String instagramUrl,
            boolean pinned,
            String category,
            Long performanceId,
            Long boothId
    ) {
        this.title = title;
        this.content = content;
        this.instagramUrl = normalize(instagramUrl);
        this.pinned = pinned;
        this.category = category;
        this.performanceId = performanceId;
        this.boothId = boothId;
        this.viewCount = 0;
    }

    public static Notice create(
            String title,
            String content,
            String instagramUrl,
            boolean pinned,
            String category,
            Long performanceId,
            Long boothId
    ) {
        return new Notice(
                title.trim(),
                content.trim(),
                instagramUrl,
                pinned,
                normalize(category),
                performanceId,
                boothId
        );
    }

    public void update(
            String title,
            String content,
            String instagramUrl,
            boolean pinned,
            String category,
            Long performanceId,
            Long boothId
    ) {
        this.title = title.trim();
        this.content = content.trim();
        this.instagramUrl = normalize(instagramUrl);
        this.pinned = pinned;
        this.category = normalize(category);
        this.performanceId = performanceId;
        this.boothId = boothId;
    }

    public void replaceImages(List<NoticeImage> newImages) {
        images.clear();
        if (newImages == null || newImages.isEmpty()) {
            return;
        }
        newImages.forEach(this::addImage);
    }

    public void addImage(NoticeImage image) {
        image.assignNotice(this);
        images.add(image);
    }

    public boolean hasImage() {
        return !images.isEmpty();
    }

    public String getPrimaryImageUrl() {
        if (images.isEmpty()) {
            return null;
        }
        return images.get(0).getImageUrl();
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

    public boolean isPinned() {
        return pinned;
    }

    public String getInstagramUrl() {
        return instagramUrl;
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

    public List<NoticeImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}
