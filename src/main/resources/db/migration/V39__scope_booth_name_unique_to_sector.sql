-- 부스 이름 유일성을 구역(sector) 범위로 한정한다 (BAC-144 / B-A-02).
-- 같은 이름이라도 구역이 다르면 허용 — 전역 UNIQUE(name) 을 제거하고 복합 UNIQUE(name, sector) 로 교체.
-- 기존 데이터는 name 이 전역 유일(uq_booths_name)이므로 (name, sector) 도 자동으로 유일 → ADD 성공.
-- 주의: MySQL UNIQUE 는 NULL 을 서로 다른 값으로 취급하므로, sector 가 NULL 인 동명 부스는 허용된다
--       (구역 미지정 상태는 충돌 판단 보류, 구역 지정 시점에 UNIQUE(name, sector) 로 정합성 회복).
ALTER TABLE booths DROP INDEX uq_booths_name;
ALTER TABLE booths ADD CONSTRAINT uq_booths_name_sector UNIQUE (name, sector);
