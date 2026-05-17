package com.likelion.yonsei.daedongje.domain.booth.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "booth_click_logs")
public class BoothClickLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_id", nullable = false)
    private Long boothId;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    protected BoothClickLog() {
    }

    private BoothClickLog(Long boothId, LocalDateTime clickedAt) {
        this.boothId = boothId;
        this.clickedAt = clickedAt;
    }

    public static BoothClickLog create(Long boothId, LocalDateTime clickedAt) {
        return new BoothClickLog(boothId, clickedAt);
    }
}
