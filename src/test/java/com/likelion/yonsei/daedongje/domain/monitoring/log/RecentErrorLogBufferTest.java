package com.likelion.yonsei.daedongje.domain.monitoring.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentErrorLogBufferTest {

    private ErrorLogEntry entry(String message) {
        return new ErrorLogEntry(LocalDateTime.now(), "ERROR", "TestLogger", message, null);
    }

    @Test
    @DisplayName("add한 항목은 최신순(newest first)으로 스냅샷에 담긴다")
    void snapshotIsNewestFirst() {
        RecentErrorLogBuffer buffer = new RecentErrorLogBuffer(3);

        buffer.add(entry("first"));
        buffer.add(entry("second"));

        List<ErrorLogEntry> snapshot = buffer.snapshot();
        assertThat(snapshot).extracting(ErrorLogEntry::message).containsExactly("second", "first");
    }

    @Test
    @DisplayName("용량을 초과하면 가장 오래된 항목을 축출한다")
    void evictsOldestBeyondCapacity() {
        RecentErrorLogBuffer buffer = new RecentErrorLogBuffer(2);

        buffer.add(entry("a"));
        buffer.add(entry("b"));
        buffer.add(entry("c"));

        assertThat(buffer.snapshot()).extracting(ErrorLogEntry::message).containsExactly("c", "b");
    }

    @Test
    @DisplayName("clear는 버퍼를 비운다")
    void clearEmptiesBuffer() {
        RecentErrorLogBuffer buffer = new RecentErrorLogBuffer(5);
        buffer.add(entry("x"));

        buffer.clear();

        assertThat(buffer.snapshot()).isEmpty();
    }
}
