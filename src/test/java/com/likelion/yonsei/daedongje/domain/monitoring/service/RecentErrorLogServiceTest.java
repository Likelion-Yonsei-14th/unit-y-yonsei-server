package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentErrorLogServiceTest {

    @Test
    @DisplayName("recent()는 싱글톤 버퍼의 최신순 스냅샷을 반환한다")
    void recentReturnsBufferSnapshot() {
        RecentErrorLogBuffer.getInstance().clear();
        RecentErrorLogBuffer.getInstance()
                .add(new ErrorLogEntry(LocalDateTime.now(), "ERROR", "L", "msg", null));

        RecentErrorLogService service = new RecentErrorLogService();

        List<ErrorLogEntry> recent = service.recent();
        assertThat(recent).extracting(ErrorLogEntry::message).containsExactly("msg");
    }
}
