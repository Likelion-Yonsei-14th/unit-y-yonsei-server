package com.likelion.yonsei.daedongje.domain.reservation.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @Column(name = "reservation_number", nullable = false)
    private Integer reservationNumber;

    @Column(name = "booker_name", nullable = false, length = 20)
    private String bookerName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(length = 4)
    private String pin;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    protected Reservation() {}

    private Reservation(Booth booth, Integer reservationNumber, String bookerName,
                        String phoneNumber, Integer partySize, String pin) {
        this.booth = booth;
        this.reservationNumber = reservationNumber;
        this.bookerName = bookerName;
        this.phoneNumber = phoneNumber;
        this.partySize = partySize;
        this.pin = pin;
        this.status = ReservationStatus.PENDING;
    }

    public static Reservation create(Booth booth, Integer reservationNumber, String bookerName,
                                     String phoneNumber, Integer partySize, String pin) {
        return new Reservation(booth, reservationNumber, bookerName, phoneNumber, partySize, pin);
    }

    // PIN이 없는 예약은 이름+연락처만으로 조회 가능
    // PIN이 있는 예약은 PIN이 일치해야 조회 가능
    public boolean matchesPin(String providedPin) {
        if (this.pin == null) {
            return true;
        }
        return this.pin.equals(providedPin);
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel(String cancelReason) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = cancelReason;
    }
}
