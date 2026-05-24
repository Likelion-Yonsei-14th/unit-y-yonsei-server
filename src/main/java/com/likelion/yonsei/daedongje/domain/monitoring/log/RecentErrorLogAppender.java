package com.likelion.yonsei.daedongje.domain.monitoring.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * ERROR 레벨 로그를 {@link RecentErrorLogBuffer} 싱글톤에 적재하는 Logback 어펜더.
 * logback-spring.xml 에서 ThresholdFilter(ERROR)와 함께 등록한다.
 */
public class RecentErrorLogAppender extends AppenderBase<ILoggingEvent> {

    private static final ZoneId FESTIVAL_ZONE = ZoneId.of("Asia/Seoul");

    @Override
    protected void append(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        String throwable = (throwableProxy == null)
                ? null
                : throwableProxy.getClassName() + ": " + throwableProxy.getMessage();

        ErrorLogEntry entry = new ErrorLogEntry(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), FESTIVAL_ZONE),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getFormattedMessage(),
                throwable
        );
        RecentErrorLogBuffer.getInstance().add(entry);
    }
}
