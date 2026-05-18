package com.likelion.yonsei.daedongje.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j 
@Service
@RequiredArgsConstructor
public class AdminSessionService {

    @Autowired(required = false)
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public void invalidateAdminSessions(Long adminUserId) {
        if (sessionRepository == null) {    // 세션 저장소가 없는 경우(세션 로딩 실패)
            log.warn("SessionRepository not available - skipping session invalidation");
            return;
        }

        String principal = String.valueOf(adminUserId);
        Map<String, ? extends Session> sessions = sessionRepository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                principal
        );

        // 세션이 존재하는 경우에 로그 남기고 삭제
        if (!sessions.isEmpty()) {
            log.info("Found {} sessions for principal: {}", sessions.size(), principal);
        }
        sessions.keySet().forEach(sessionRepository::deleteById);
    }
}
