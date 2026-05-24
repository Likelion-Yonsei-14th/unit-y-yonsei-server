package com.likelion.yonsei.daedongje.domain.monitoring.service;

import com.likelion.yonsei.daedongje.domain.monitoring.log.ErrorLogEntry;
import com.likelion.yonsei.daedongje.domain.monitoring.log.RecentErrorLogBuffer;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link RecentErrorLogBuffer} 싱글톤을 읽는 스프링 빈.
 * 어펜더는 싱글톤에 write, 이 빈은 read만 한다.
 */
@Service
public class RecentErrorLogService {

    public List<ErrorLogEntry> recent() {
        return RecentErrorLogBuffer.getInstance().snapshot();
    }
}
