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
    display_order  INT           NOT NULL DEFAULT 0,
    display_status VARCHAR(20),
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_map_locations_filters (sector, location_type, display_status),
    INDEX idx_map_locations_display_order (display_order, id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

ALTER TABLE booths
    ADD INDEX idx_booths_location_id (location_id),
    ADD CONSTRAINT fk_booths_map_location
        FOREIGN KEY (location_id) REFERENCES map_locations (id);
