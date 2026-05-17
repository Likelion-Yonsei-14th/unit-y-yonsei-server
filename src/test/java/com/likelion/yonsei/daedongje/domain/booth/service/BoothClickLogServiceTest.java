package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothClickLog;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothClickLogRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoothClickLogServiceTest {

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private BoothClickLogRepository boothClickLogRepository;

    @InjectMocks
    private BoothClickLogService boothClickLogService;

    @Test
    @DisplayName("존재하는 부스의 클릭 로그를 저장한다")
    void createSavesClickLog() {
        when(boothRepository.existsById(1L)).thenReturn(true);

        boothClickLogService.create(1L);

        ArgumentCaptor<BoothClickLog> captor = ArgumentCaptor.forClass(BoothClickLog.class);
        verify(boothClickLogRepository).save(captor.capture());

        BoothClickLog savedLog = captor.getValue();
        assertThat(savedLog.getBoothId()).isEqualTo(1L);
        assertThat(savedLog.getClickedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 부스면 클릭 로그를 저장하지 않고 예외를 던진다")
    void createThrowsWhenBoothNotFound() {
        when(boothRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> boothClickLogService.create(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(BoothErrorCode.BOOTH_NOT_FOUND);

        verify(boothClickLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
