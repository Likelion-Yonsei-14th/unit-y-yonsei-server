package com.likelion.yonsei.daedongje.domain.monitoring.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 최근 ERROR 로그를 메모리에 보관하는 스레드세이프 싱글톤 링버퍼.
 *
 * <p>{@link RecentErrorLogAppender}(Logback)가 write하고
 * {@code RecentErrorLogService}가 read한다. 용량 초과 시 가장 오래된 항목을 축출한다.
 * 이 인스턴스 한정·재시작 시 휘발(영속 로그는 Loki가 담당).
 */
public final class RecentErrorLogBuffer {

    public static final int DEFAULT_CAPACITY = 100;

    private static final RecentErrorLogBuffer INSTANCE = new RecentErrorLogBuffer(DEFAULT_CAPACITY);

    private final int capacity;
    private final Deque<ErrorLogEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger(0);

    // 테스트에서 용량을 지정하기 위해 package-private.
    RecentErrorLogBuffer(int capacity) {
        this.capacity = capacity;
    }

    public static RecentErrorLogBuffer getInstance() {
        return INSTANCE;
    }

    public void add(ErrorLogEntry entry) {
        entries.addFirst(entry);
        if (size.incrementAndGet() > capacity && entries.pollLast() != null) {
            size.decrementAndGet();
        }
    }

    /** 최신순(newest first) 불변 스냅샷. */
    public List<ErrorLogEntry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public void clear() {
        entries.clear();
        size.set(0);
    }
}
