-- info 도메인에서 이미 컬럼으로 들고 있던 참조 관계를 FK로 보강한다.
-- notice.performance_id 는 아직 dev 기준 대상 테이블이 없어 이번 마이그레이션에서 제외한다.

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
    ADD CONSTRAINT fk_notice_booth
        FOREIGN KEY (booth_id) REFERENCES booths (id);
