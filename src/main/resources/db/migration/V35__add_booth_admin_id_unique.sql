-- "계정당 부스 1개" 정책을 DB 레벨에서 강제 — 동시 생성(TOCTOU) 시 애플리케이션 선검증을
-- 통과한 중복 부스가 저장되는 것을 막는다. (admin_id 는 V3 에서 FK 없는 숫자 컬럼으로 추가됨)
-- 선행 조건: 기존 booths 에 admin_id 중복 행이 없어야 한다. 있으면 정리 후 적용한다.
ALTER TABLE booths ADD CONSTRAINT uq_booths_admin_id UNIQUE (admin_id);
