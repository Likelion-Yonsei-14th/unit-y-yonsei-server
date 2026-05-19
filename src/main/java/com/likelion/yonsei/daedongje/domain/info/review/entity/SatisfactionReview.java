package com.likelion.yonsei.daedongje.domain.info.review.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "satisfaction_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SatisfactionReview extends BaseEntity {

    public static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content", length = MAX_CONTENT_LENGTH)
    private String content;

    private SatisfactionReview(Integer rating, String content) {
        this.rating = rating;
        this.content = normalizeContent(content);
    }

    public static SatisfactionReview create(Integer rating, String content) {
        return new SatisfactionReview(rating, content);
    }

    private static String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.trim();
    }
}
