package com.likelion.yonsei.daedongje.domain.reservation.service;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateRequest;
import com.likelion.yonsei.daedongje.domain.reservation.dto.ReservationCreateResponse;
import com.likelion.yonsei.daedongje.domain.reservation.entity.Reservation;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("최근 중복 예약이 없으면 신규 예약을 생성한다")
    void createsNewReservationWhenNoRecentDuplicate() {
        Booth booth = booth(3L);
        when(boothRepository.findByIdWithLock(3L)).thenReturn(Optional.of(booth));
        when(reservationRepository.findRecentDuplicates(eq(3L), eq("010-1234-5678"),
                eq(ReservationStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(reservationRepository.findMaxReservationNumberByBoothId(3L)).thenReturn(Optional.of(4));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.countByBoothIdAndStatus(3L, ReservationStatus.PENDING)).thenReturn(5L);

        ReservationCreateResponse response = reservationService.create(3L, request());

        assertThat(response.getReservationNumber()).isEqualTo(5);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("최근 10초 내 같은 전화번호의 PENDING 예약이 있으면 신규 생성 없이 기존 예약을 반환한다")
    void returnsExistingReservationWhenRecentDuplicateExists() {
        Booth booth = booth(3L);
        Reservation existing = Reservation.create(booth, 7, "홍길동", "010-1234-5678", 2, null);
        ReflectionTestUtils.setField(existing, "id", 99L);
        when(boothRepository.findByIdWithLock(3L)).thenReturn(Optional.of(booth));
        when(reservationRepository.findRecentDuplicates(eq(3L), eq("010-1234-5678"),
                eq(ReservationStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));
        when(reservationRepository.countByBoothIdAndStatus(3L, ReservationStatus.PENDING)).thenReturn(5L);

        ReservationCreateResponse response = reservationService.create(3L, request());

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getReservationNumber()).isEqualTo(7);
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(reservationRepository, never()).findMaxReservationNumberByBoothId(any());
    }

    @Test
    @DisplayName("멱등 조회는 PENDING 상태와 약 10초 윈도우로 수행된다")
    void queriesDuplicatesWithPendingStatusAndTenSecondWindow() {
        Booth booth = booth(3L);
        when(boothRepository.findByIdWithLock(3L)).thenReturn(Optional.of(booth));
        when(reservationRepository.findRecentDuplicates(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservationRepository.findMaxReservationNumberByBoothId(3L)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.countByBoothIdAndStatus(3L, ReservationStatus.PENDING)).thenReturn(1L);

        LocalDateTime before = LocalDateTime.now();
        reservationService.create(3L, request());
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<ReservationStatus> statusCaptor = ArgumentCaptor.forClass(ReservationStatus.class);
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reservationRepository).findRecentDuplicates(eq(3L), eq("010-1234-5678"),
                statusCaptor.capture(), sinceCaptor.capture());

        assertThat(statusCaptor.getValue()).isEqualTo(ReservationStatus.PENDING);
        assertThat(sinceCaptor.getValue())
                .isAfterOrEqualTo(before.minusSeconds(10))
                .isBeforeOrEqualTo(after.minusSeconds(10));
    }

    private ReservationCreateRequest request() {
        return new ReservationCreateRequest("홍길동", "010-1234-5678", 2, null, true);
    }

    private Booth booth(Long id) {
        Booth booth = Booth.create(
                1L, "멋사 핫도그", "멋쟁이사자처럼", "소개",
                2, LocalTime.of(11, 0), LocalTime.of(20, 0),
                BoothSector.한글탑, 3, BoothStatus.OPEN,
                true, null, true, null,
                null, null, false, null
        );
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }
}
