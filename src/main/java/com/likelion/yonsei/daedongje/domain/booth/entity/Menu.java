package com.likelion.yonsei.daedongje.domain.booth.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 부스 메뉴 정보를 저장하는 엔티티
@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 메뉴가 속한 부스
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    // 메뉴 이름
    @Column(nullable = false, length = 255)
    private String name;

    // 메뉴 설명
    @Column(length = 255)
    private String description;

    // 메뉴 가격
    @Column(nullable = false)
    private Integer price;

    // 메뉴 이미지 URL
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    // 품절 여부
    @Column(name = "is_sold_out", nullable = false)
    private Boolean isSoldOut = false;

    // 메뉴 표시 순서
    @Column(name = "display_order")
    private Integer displayOrder;

    // 메뉴 생성자
    @Builder
    public Menu(
            Booth booth,
            String name,
            String description,
            Integer price,
            String imageUrl,
            Boolean isSoldOut,
            Integer displayOrder) {
        this.booth = booth;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isSoldOut = isSoldOut != null ? isSoldOut : false;
        this.displayOrder = displayOrder;
    }

    // 메뉴 정보 수정
    // -- 입력이 null일 경우 현재값 유지
    public void update(
            String name,
            String description,
            Integer price,
            String imageUrl,
            Boolean isSoldOut,
            Integer displayOrder) {
        if (name != null) {
            this.name = name;
        }

        if (description != null) {
            this.description = description;
        }

        if (price != null) {
            this.price = price;
        }

        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }

        if (isSoldOut != null) {
            this.isSoldOut = isSoldOut;
        }

        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    // 품절 상태만 수정
    public void updateSoldOut(Boolean isSoldOut) {
        this.isSoldOut = isSoldOut != null ? isSoldOut : false;
    }
}