CREATE TABLE map_locations
(
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    location_name  VARCHAR(100),
    sector         VARCHAR(10),
    map_x          DECIMAL(10, 4),
    map_y          DECIMAL(10, 4),
    width          DECIMAL(6, 3),
    height         DECIMAL(6, 3),
    location_type  VARCHAR(30),
    display_order  INT,
    display_status VARCHAR(20),
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_map_locations_filters (sector, location_type, display_status),
    INDEX idx_map_locations_display_order (display_order, id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
