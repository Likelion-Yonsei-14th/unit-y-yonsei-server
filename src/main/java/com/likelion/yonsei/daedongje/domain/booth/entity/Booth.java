package com.likelion.yonsei.daedongje.domain.booth.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Entity
@Table(name = "booths")
public class Booth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String organization;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Integer date;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private BoothSector sector;

    // 각 섹터별 부스 배치 번호
    @Column
    private Integer location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BoothStatus status;

    @Column(name = "is_food", nullable = false)
    private Boolean isFood;

    @Column(columnDefinition = "TEXT")
    private String instagram;

    @Column(name = "is_reservable", nullable = false)
    private Boolean isReservable;

    @Column(columnDefinition = "TEXT")
    private String account;

    // 지도 위치 엔티티 참조 ID (FK 제약은 해당 테이블 완성 후 별도 마이그레이션 예정)
    @Column(name = "location_id")
    private Long locationId;

    protected Booth() {}

    private Booth(Long adminId, String name, String organization, String description,
                  Integer date, LocalTime openTime, LocalTime closeTime,
                  BoothSector sector, Integer location, BoothStatus status,
                  Boolean isFood, String instagram, Boolean isReservable,
                  String account, Long locationId) {
        this.adminId = adminId;
        this.name = name;
        this.organization = organization;
        this.description = description;
        this.date = date;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.sector = sector;
        this.location = location;
        this.status = status;
        this.isFood = isFood;
        this.instagram = instagram;
        this.isReservable = isReservable;
        this.account = account;
        this.locationId = locationId;
    }

    public static Booth create(Long adminId, String name, String organization, String description,
                               Integer date, LocalTime openTime, LocalTime closeTime,
                               BoothSector sector, Integer location, BoothStatus status,
                               Boolean isFood, String instagram, Boolean isReservable,
                               String account, Long locationId) {
        return new Booth(adminId, name, organization, description, date, openTime, closeTime,
                sector, location, status, isFood, instagram, isReservable, account, locationId);
    }

    public void update(String name, String organization, String description,
                       Integer date, LocalTime openTime, LocalTime closeTime,
                       BoothSector sector, Integer location, BoothStatus status,
                       Boolean isFood, String instagram, Boolean isReservable,
                       String account, Long locationId) {
        this.name = name;
        this.organization = organization;
        this.description = description;
        this.date = date;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.sector = sector;
        this.location = location;
        this.status = status;
        this.isFood = isFood;
        this.instagram = instagram;
        this.isReservable = isReservable;
        this.account = account;
        this.locationId = locationId;
    }

    public void updateStatus(BoothStatus status) {
        this.status = status;
    }

    public void updateIsReservable(boolean isReservable) {
        this.isReservable = isReservable;
    }

    /**
     * 부스 프로필 작성 완료 여부를 반환한다.
     * organization, date, openTime, closeTime, sector, location 이 모두 채워진 경우 true.
     */
    public boolean isProfileComplete() {
        return organization != null && !organization.isBlank()
                && date != null
                && openTime != null
                && closeTime != null
                && sector != null
                && location != null;
    }
}
