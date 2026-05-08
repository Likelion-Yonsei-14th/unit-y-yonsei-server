package com.likelion.yonsei.daedongje.domain.booth.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalTime;

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

    @Column(nullable = false, length = 100)
    private String organization;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer date;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false, length = 10)
    private String sector;

    @Column(nullable = false)
    private Integer location;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "is_food", nullable = false)
    private Boolean isFood;

    @Column(columnDefinition = "TEXT")
    private String instagram;

    @Column(name = "is_reservable", nullable = false)
    private Boolean isReservable;

    @Column(columnDefinition = "TEXT")
    private String account;

    @Column(name = "location_id")
    private Long locationId;

    protected Booth() {}

    private Booth(Long adminId, String name, String organization, String description,
                  Integer date, LocalTime openTime, LocalTime closeTime,
                  String sector, Integer location, String status,
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
                               String sector, Integer location, String status,
                               Boolean isFood, String instagram, Boolean isReservable,
                               String account, Long locationId) {
        return new Booth(adminId, name, organization, description, date, openTime, closeTime,
                sector, location, status, isFood, instagram, isReservable, account, locationId);
    }

    public void update(String name, String organization, String description,
                       Integer date, LocalTime openTime, LocalTime closeTime,
                       String sector, Integer location, String status,
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

    public Long getId() { return id; }
    public Long getAdminId() { return adminId; }
    public String getName() { return name; }
    public String getOrganization() { return organization; }
    public String getDescription() { return description; }
    public Integer getDate() { return date; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public String getSector() { return sector; }
    public Integer getLocation() { return location; }
    public String getStatus() { return status; }
    public Boolean getIsFood() { return isFood; }
    public String getInstagram() { return instagram; }
    public Boolean getIsReservable() { return isReservable; }
    public String getAccount() { return account; }
    public Long getLocationId() { return locationId; }
}
