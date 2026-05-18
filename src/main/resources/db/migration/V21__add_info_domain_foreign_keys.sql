-- info 도메인에서 이미 컬럼으로 들고 있던 참조 관계를 FK로 보강한다.
-- MySQL DDL은 문장별 auto-commit 이므로, FK 추가 전에 orphan 참조를 먼저 검증해
-- 중간까지 반영된 상태가 남는 위험을 줄인다.

DROP PROCEDURE IF EXISTS validate_info_domain_fk_references;

DELIMITER $$

CREATE PROCEDURE validate_info_domain_fk_references()
BEGIN
    DECLARE barrier_orphan_count BIGINT DEFAULT 0;
    DECLARE lost_found_orphan_count BIGINT DEFAULT 0;
    DECLARE lost_storage_orphan_count BIGINT DEFAULT 0;
    DECLARE notice_booth_orphan_count BIGINT DEFAULT 0;
    DECLARE notice_performance_orphan_count BIGINT DEFAULT 0;
    DECLARE validation_message TEXT;

    SELECT COUNT(*)
    INTO barrier_orphan_count
    FROM barrier_free_infos b
    LEFT JOIN map_locations m ON b.map_location_id = m.id
    WHERE m.id IS NULL;

    SELECT COUNT(*)
    INTO lost_found_orphan_count
    FROM lost_items l
    LEFT JOIN map_locations m ON l.found_location_id = m.id
    WHERE l.found_location_id IS NOT NULL
      AND m.id IS NULL;

    SELECT COUNT(*)
    INTO lost_storage_orphan_count
    FROM lost_items l
    LEFT JOIN map_locations m ON l.storage_location_id = m.id
    WHERE l.storage_location_id IS NOT NULL
      AND m.id IS NULL;

    SELECT COUNT(*)
    INTO notice_booth_orphan_count
    FROM notice n
    LEFT JOIN booths b ON n.booth_id = b.id
    WHERE n.booth_id IS NOT NULL
      AND b.id IS NULL;

    SELECT COUNT(*)
    INTO notice_performance_orphan_count
    FROM notice n
    LEFT JOIN performances p ON n.performance_id = p.id
    WHERE n.performance_id IS NOT NULL
      AND p.id IS NULL;

    IF barrier_orphan_count > 0
        OR lost_found_orphan_count > 0
        OR lost_storage_orphan_count > 0
        OR notice_booth_orphan_count > 0
        OR notice_performance_orphan_count > 0 THEN
        SET validation_message = CONCAT(
            'Cannot add info-domain foreign keys because orphan references exist. ',
            'barrier_free_infos.map_location_id=', barrier_orphan_count,
            ', lost_items.found_location_id=', lost_found_orphan_count,
            ', lost_items.storage_location_id=', lost_storage_orphan_count,
            ', notice.booth_id=', notice_booth_orphan_count,
            ', notice.performance_id=', notice_performance_orphan_count
        );

        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = validation_message;
    END IF;
END$$

DELIMITER ;

CALL validate_info_domain_fk_references();

DROP PROCEDURE validate_info_domain_fk_references;

ALTER TABLE barrier_free_infos
    ADD INDEX idx_barrier_free_infos_map_location_id (map_location_id),
    ADD CONSTRAINT fk_barrier_free_infos_map_location
        FOREIGN KEY (map_location_id) REFERENCES map_locations (id);

ALTER TABLE lost_items
    ADD INDEX idx_lost_items_found_location_id (found_location_id),
    ADD INDEX idx_lost_items_storage_location_id (storage_location_id),
    ADD CONSTRAINT fk_lost_items_found_location
        FOREIGN KEY (found_location_id) REFERENCES map_locations (id),
    ADD CONSTRAINT fk_lost_items_storage_location
        FOREIGN KEY (storage_location_id) REFERENCES map_locations (id);

ALTER TABLE notice
    ADD INDEX idx_notice_booth_id (booth_id),
    ADD INDEX idx_notice_performance_id (performance_id),
    ADD CONSTRAINT fk_notice_booth
        FOREIGN KEY (booth_id) REFERENCES booths (id),
    ADD CONSTRAINT fk_notice_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id);
