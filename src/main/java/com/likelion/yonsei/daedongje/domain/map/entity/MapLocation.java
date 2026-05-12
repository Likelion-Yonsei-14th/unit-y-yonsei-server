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

    @Column(name = "location_type", length = 30)
    private String locationType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "display_status", length = 20)
    private String displayStatus;

    protected MapLocation() {
    }
}
