package com.likelion.yonsei.daedongje.domain.home.service;

import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothSector;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothImageRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.PopularBoothSummary;
import com.likelion.yonsei.daedongje.domain.home.dto.HomePopularBoothResponse;
import com.likelion.yonsei.daedongje.domain.reservation.entity.ReservationStatus;
import com.likelion.yonsei.daedongje.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private BoothClickLogRepository boothClickLogRepository;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private BoothImageRepository boothImageRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private HomeService homeService;

    @Test
    @DisplayName("인기 부스 집계 결과에 부스 정보와 대기 팀 수, 썸네일을 붙여 반환한다")
    void getPopularBoothsReturnsBoothDetails() {
        when(boothClickLogRepository.findPopularBooths(any(), any(), any()))
                .thenReturn(List.of(summary(3L, 10L), summary(1L, 7L)));
        when(boothRepository.findAllById(List.of(3L, 1L)))
                .thenReturn(List.of(
                        booth(1L, "분식 한 접시", "국문학과", BoothSector.한글탑, 2, "떡볶이, 순대"),
                        booth(3L, "호프 한 잔", "사회학과", BoothSector.백양로, 7, "치킨, 맥주")
                ));
        when(reservationRepository.countByBoothIdsAndStatus(List.of(3L, 1L), ReservationStatus.PENDING))
                .thenReturn(List.<Object[]>of(new Object[]{3L, 2L}));
        when(boothImageRepository.findThumbnailsByBoothIds(List.of(3L, 1L)))
                .thenReturn(List.of(boothImage(3L, "https://example.com/thumbnail.jpg")));

        List<HomePopularBoothResponse> responses = homeService.getPopularBooths();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getRank()).isEqualTo(1);
        assertThat(responses.get(0).getBoothId()).isEqualTo(3L);
        assertThat(responses.get(0).getClickCount()).isEqualTo(10L);
        assertThat(responses.get(0).getName()).isEqualTo("호프 한 잔");
        assertThat(responses.get(0).getOrganization()).isEqualTo("사회학과");
        assertThat(responses.get(0).getSector()).isEqualTo(BoothSector.백양로);
        assertThat(responses.get(0).getLocation()).isEqualTo(7);
        assertThat(responses.get(0).getStatus()).isEqualTo(BoothStatus.OPEN);
        assertThat(responses.get(0).getIsFood()).isTrue();
        assertThat(responses.get(0).getIsReservable()).isTrue();
        assertThat(responses.get(0).getRepresentativeMenus()).containsExactly("치킨", "맥주");
        assertThat(responses.get(0).getWaitingCount()).isEqualTo(2L);
        assertThat(responses.get(0).getThumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
        assertThat(responses.get(1).getRank()).isEqualTo(2);
        assertThat(responses.get(1).getBoothId()).isEqualTo(1L);
        assertThat(responses.get(1).getClickCount()).isEqualTo(7L);
        assertThat(responses.get(1).getWaitingCount()).isZero();
        assertThat(responses.get(1).getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("인기 부스는 TOP 5 제한으로 조회한다")
    void getPopularBoothsUsesTopFiveLimit() {
        when(boothClickLogRepository.findPopularBooths(any(), any(), any()))
                .thenReturn(List.of());

        homeService.getPopularBooths();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(boothClickLogRepository).findPopularBooths(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    private Booth booth(Long id, String name, String organization, BoothSector sector,
                        Integer location, String representativeMenus) {
        Booth booth = Booth.create(
                1L,
                name,
                organization,
                "소개",
                2,
                LocalTime.of(11, 0),
                LocalTime.of(20, 0),
                sector,
                location,
                BoothStatus.OPEN,
                true,
                null,
                true,
                null,
                null,
                representativeMenus
        );
        org.springframework.test.util.ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private BoothImage boothImage(Long boothId, String imageUrl) {
        return BoothImage.create(boothId, imageUrl, 1);
    }

    private PopularBoothSummary summary(Long boothId, Long clickCount) {
        return new PopularBoothSummary() {
            @Override
            public Long getBoothId() {
                return boothId;
            }

            @Override
            public Long getClickCount() {
                return clickCount;
            }
        };
    }
}
