-- booths.name 중복 저장 방지를 위한 UNIQUE 제약 추가
ALTER TABLE booths ADD CONSTRAINT uq_booths_name UNIQUE (name);
