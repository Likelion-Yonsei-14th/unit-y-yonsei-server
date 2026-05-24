package com.likelion.yonsei.daedongje.domain.monitoring.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
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

    @Test
    @DisplayName("append는 예외 정보를 'ClassName: message' 형태로 변환한다")
    void appendFormatsThrowableWithMessage() {
        RecentErrorLogBuffer.getInstance().clear();

        IThrowableProxy proxy = mock(IThrowableProxy.class);
        when(proxy.getClassName()).thenReturn("java.lang.IllegalStateException");
        when(proxy.getMessage()).thenReturn("bad state");

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getLoggerName()).thenReturn("com.example.Foo");
        when(event.getFormattedMessage()).thenReturn("something failed");
        when(event.getThrowableProxy()).thenReturn(proxy);

        new RecentErrorLogAppender().append(event);

        ErrorLogEntry entry = RecentErrorLogBuffer.getInstance().snapshot().get(0);
        assertThat(entry.throwable()).isEqualTo("java.lang.IllegalStateException: bad state");
    }

    @Test
    @DisplayName("예외 메시지가 null이면 throwable은 클래스명만 담는다 (': null' 없음)")
    void appendFormatsThrowableWithoutMessage() {
        RecentErrorLogBuffer.getInstance().clear();

        IThrowableProxy proxy = mock(IThrowableProxy.class);
        when(proxy.getClassName()).thenReturn("java.lang.NullPointerException");
        when(proxy.getMessage()).thenReturn(null);

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getLoggerName()).thenReturn("com.example.Foo");
        when(event.getFormattedMessage()).thenReturn("npe happened");
        when(event.getThrowableProxy()).thenReturn(proxy);

        new RecentErrorLogAppender().append(event);

        ErrorLogEntry entry = RecentErrorLogBuffer.getInstance().snapshot().get(0);
        assertThat(entry.throwable()).isEqualTo("java.lang.NullPointerException");
    }
}
