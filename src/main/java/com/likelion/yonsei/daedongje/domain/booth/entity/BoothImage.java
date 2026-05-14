package com.likelion.yonsei.daedongje.domain.booth.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "booth_images")
public class BoothImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_id", nullable = false)
    private Long boothId;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    protected BoothImage() {}

    private BoothImage(Long boothId, String imageUrl, Integer displayOrder) {
        this.boothId = boothId;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public static BoothImage create(Long boothId, String imageUrl, Integer displayOrder) {
        return new BoothImage(boothId, imageUrl, displayOrder);
    }

    public void update(String imageUrl, Integer displayOrder) {
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}