package com.likelion.yonsei.daedongje.domain.map.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "map_locations")
public class MapLocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_name", length = 100)
    private String locationName;

    @Column(length = 10)
    private String sector;

    @Column(name = "map_x", precision = 10, scale = 4)
    private BigDecimal mapX;

    @Column(name = "map_y", precision = 10, scale = 4)
    private BigDecimal mapY;

    @Column(precision = 6, scale = 3)
    private BigDecimal width;

    @Column(precision = 6, scale = 3)
    private BigDecimal height;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 30)
    private MapLocationType locationType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", length = 20)
    private MapDisplayStatus displayStatus;

    protected MapLocation() {
    }

    private MapLocation(
            String locationName,
            String sector,
            BigDecimal mapX,
            BigDecimal mapY,
            BigDecimal width,
            BigDecimal height,
            MapLocationType locationType,
            Integer displayOrder,
            MapDisplayStatus displayStatus
    ) {
        this.locationName = locationName;
        this.sector = sector;
        this.mapX = mapX;
        this.mapY = mapY;
        this.width = width;
        this.height = height;
        this.locationType = locationType;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.displayStatus = displayStatus;
    }

    public static MapLocation create(
            String locationName,
            String sector,
            BigDecimal mapX,
            BigDecimal mapY,
            BigDecimal width,
            BigDecimal height,
            MapLocationType locationType,
            Integer displayOrder,
            MapDisplayStatus displayStatus
    ) {
        return new MapLocation(
                locationName,
                sector,
                mapX,
                mapY,
                width,
                height,
                locationType,
                displayOrder,
                displayStatus
        );
    }
}
