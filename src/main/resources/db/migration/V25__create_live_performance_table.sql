-- 운영진이 수동 지정하는 '현재 라이브 공연' 단일 포인터.
-- 전역 단일 값이므로 행 하나만 두고, performance_id 가 NULL 이면 미지정 상태.
CREATE TABLE live_performance (
    id             BIGINT PRIMARY KEY,
    performance_id BIGINT NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_live_performance_performance
        FOREIGN KEY (performance_id)
        REFERENCES performances(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 서비스가 항상 의존할 수 있도록 싱글톤 행(id = 1)을 미리 시드한다.
INSERT INTO live_performance (id, performance_id, created_at, updated_at)
VALUES (1, NULL, NOW(6), NOW(6));
