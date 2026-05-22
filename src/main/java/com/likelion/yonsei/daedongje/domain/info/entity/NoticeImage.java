package com.likelion.yonsei.daedongje.domain.info.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notice_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    private NoticeImage(String imageUrl, Integer displayOrder) {
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public static NoticeImage create(String imageUrl, Integer displayOrder) {
        return new NoticeImage(imageUrl, displayOrder);
    }

    void assignNotice(Notice notice) {
        this.notice = notice;
    }
}
