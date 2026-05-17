CREATE TABLE performances
(
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    created_by              BIGINT        NOT NULL,
    admin_id                BIGINT        NOT NULL,
    location_id             BIGINT,
    performance_name        VARCHAR(100)  NOT NULL,
    performance_description TEXT,
    performance_date        INT,
    start_time              TIME,
    end_time                TIME,
    performance_category    VARCHAR(50),
    lineup_name             VARCHAR(100),
    performance_status      VARCHAR(20)   NOT NULL DEFAULT 'HIDDEN',
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_performances_admin_id (admin_id),
    INDEX idx_performances_location_id (location_id),
    CONSTRAINT fk_performances_admin_user
        FOREIGN KEY (admin_id) REFERENCES admin_users (id),
    CONSTRAINT fk_performances_map_location
        FOREIGN KEY (location_id) REFERENCES map_locations (id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
