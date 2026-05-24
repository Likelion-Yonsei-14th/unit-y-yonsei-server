package com.likelion.yonsei.daedongje.domain.monitoring.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecentErrorLogAppenderTest {

    @Test
    @DisplayName("append는 로그 이벤트를 ErrorLogEntry로 변환해 싱글톤 버퍼에 적재한다")
    void appendStoresEntryInSingletonBuffer() {
        RecentErrorLogBuffer.getInstance().clear();

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getLoggerName()).thenReturn("com.example.Foo");
        when(event.getFormattedMessage()).thenReturn("boom");
        when(event.getThrowableProxy()).thenReturn(null);

        RecentErrorLogAppender appender = new RecentErrorLogAppender();
        appender.append(event);

        List<ErrorLogEntry> snapshot = RecentErrorLogBuffer.getInstance().snapshot();
        assertThat(snapshot).hasSize(1);
        assertThat(snapshot.get(0).message()).isEqualTo("boom");
        assertThat(snapshot.get(0).level()).isEqualTo("ERROR");
        assertThat(snapshot.get(0).logger()).isEqualTo("com.example.Foo");
        assertThat(snapshot.get(0).throwable()).isNull();
    }
}
