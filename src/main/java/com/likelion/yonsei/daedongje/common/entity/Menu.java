package com.likelion.yonsei.daedongje.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Booth 엔티티가 아직 dev 브랜치에 merge되지 않아
    // 임시로 FK(Long) 기반으로 구현.
    // 추후 Booth 엔티티 merge 이후 @ManyToOne 연관관계로 변경 예정.
    @Column(name = "booth_id", nullable = false)
    private Long boothId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_sold_out", nullable = false)
    private Boolean isSoldOut = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder
    public Menu(
            Long boothId,
            String name,
            String description,
            Integer price,
            String imageUrl,
            Boolean isSoldOut,
            Integer displayOrder) {
        this.boothId = boothId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isSoldOut = isSoldOut != null ? isSoldOut : false;
        this.displayOrder = displayOrder;
    }

    public void updateSoldOut(Boolean isSoldOut) {
        this.isSoldOut = isSoldOut;
    }
}