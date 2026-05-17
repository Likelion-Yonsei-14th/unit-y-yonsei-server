package com.likelion.yonsei.daedongje.domain.performance.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "performance_setlists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceSetlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(name = "song_title", nullable = false, length = 100)
    private String songTitle;

    @Column(name = "singer_name", nullable = false, length = 100)
    private String singerName;

    @Column(name = "song_order", nullable = false)
    private Integer songOrder;

    @Column(name = "note", length = 255)
    private String note;

    private PerformanceSetlist(
            Performance performance,
            String songTitle,
            String singerName,
            Integer songOrder,
            String note
    ) {
        this.performance = performance;
        this.songTitle = songTitle;
        this.singerName = singerName;
        this.songOrder = songOrder;
        this.note = note;
    }

    public static PerformanceSetlist create(
            Performance performance,
            String songTitle,
            String singerName,
            Integer songOrder,
            String note
    ) {
        return new PerformanceSetlist(performance, songTitle, singerName, songOrder, note);
    }

    public void update(String songTitle, String singerName, Integer songOrder, String note) {
        if (songTitle != null) {
            this.songTitle = songTitle;
        }
        if (singerName != null) {
            this.singerName = singerName;
        }
        if (songOrder != null) {
            this.songOrder = songOrder;
        }
        if (note != null) {
            this.note = note;
        }
    }
}
