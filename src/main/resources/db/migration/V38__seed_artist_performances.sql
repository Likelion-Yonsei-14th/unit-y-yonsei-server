-- =============================================================================
-- V38: 아티스트 공연 데이터 시드 (BAC-140 / P-01)
-- -----------------------------------------------------------------------------
-- 아티스트 공연(performance_category = 'ARTIST')을 DB에 시드한다.
--
-- [설계: 접근법 A] performances.admin_id 는 NOT NULL + FK + UNIQUE(공연당 계정 1개)이므로,
-- 아티스트마다 더미 PERFORMER 계정을 함께 INSERT 한다. 더미 계정은 status='INACTIVE' 로
-- 로그인을 차단한다(AdminAuthService.validateActiveStatus). 공개 공연 조회는 admin 을
-- 참조하지 않으므로 더미 계정이 화면에 영향을 주지 않는다.
--   - created_by  = super 어드민(V16 에서 시드됨)
--   - performance_status = 'SCHEDULED' (목록/타임테이블에 노출. 기본값 HIDDEN 은 숨김)
--   - performance_name = 한글명, lineup_name = 영문명
--   - 일차 체계는 2~4 (FestivalDayService: 2=5/27, 3=5/28, 4=5/29)
--   - 시작/종료 시간은 미정이라 NULL. 같은 일차 내 순서는 삽입 순서(id)로 보존됨.
--   - 이미지는 FE 더미로 처리하므로 performance_images 는 시드하지 않는다.
--
-- ▶ 아티스트 추가/수정: 아래 _artist_seed VALUES 한 줄만 추가하면 계정+공연이 함께 시드된다.
--   (login_id 는 유니크하게. WHERE NOT EXISTS 가드로 재적용 시 중복 안전.)
-- =============================================================================

-- NOTE: 의도적 고정 - 로그인 불가용 더미 해시(어차피 status=INACTIVE 로 로그인 차단됨)
SET @pw  := '$2a$10$seedLockedDummyHashForArtistAccountsNotUsable000000000000';
SET @now := NOW(6);

-- 라인업 정의(한 곳). login_id | 한글명 | 영문명 | 일차 | 무대
CREATE TEMPORARY TABLE _artist_seed (
    login_id VARCHAR(50),
    name     VARCHAR(100),
    name_en  VARCHAR(100),
    perf_day INT,
    stage    VARCHAR(100)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO _artist_seed (login_id, name, name_en, perf_day, stage) VALUES
    -- 5/28(목) = 일차 3 · 노천극장
    ('artist_01', '신인류',       'Shin In Ryu',     3, '노천극장'),
    ('artist_02', '드래곤포니',   'Dragon Pony',     3, '노천극장'),
    ('artist_03', '윤마치',       'MRCH',            3, '노천극장'),
    ('artist_04', '터치드',       'Touched',         3, '노천극장'),
    ('artist_05', '실리카겔',     'Silica Gel',      3, '노천극장'),
    ('artist_06', 'YB',          'YB',              3, '노천극장'),
    -- 5/29(금) = 일차 4 · 노천극장
    ('artist_07', '민수',         'MINSOO',          4, '노천극장'),
    ('artist_08', '오존',         'O3ohn',           4, '노천극장'),
    ('artist_09', '리도어',       'Redoor',          4, '노천극장'),
    ('artist_10', '데이먼스 이어', 'DAMONS YEAR',     4, '노천극장'),
    ('artist_11', '카더가든',     'Car, the Garden', 4, '노천극장'),
    ('artist_12', '넬',           'NEL',             4, '노천극장');

-- 1) 더미 PERFORMER 계정 (INACTIVE)
INSERT INTO admin_users
    (login_id, password_hash, organization, role, status, representative_name, representative_phone, created_at, updated_at)
SELECT s.login_id, @pw, s.name, 'PERFORMER', 'INACTIVE', s.name, '010-0000-0000', @now, @now
FROM _artist_seed s
WHERE NOT EXISTS (SELECT 1 FROM admin_users a WHERE a.login_id = s.login_id);

-- 2) 아티스트 공연 (ARTIST / SCHEDULED). 무대명이 map_locations 에 없으면 location_id = NULL.
INSERT INTO performances
    (created_by, admin_id, location_id, performance_name, performance_description, performance_date,
     start_time, end_time, performance_category, lineup_name, performance_status,
     hashtag1, hashtag2, hashtag3, youtube_url, instagram_url, created_at, updated_at)
SELECT
    (SELECT id FROM admin_users WHERE login_id = 'super'),
    a.id,
    (SELECT id FROM map_locations WHERE location_name = s.stage LIMIT 1),
    s.name,
    NULL,
    s.perf_day,
    NULL,
    NULL,
    'ARTIST',
    s.name_en,
    'SCHEDULED',
    NULL, NULL, NULL,
    NULL, NULL,
    @now, @now
FROM _artist_seed s
JOIN admin_users a ON a.login_id = s.login_id
WHERE NOT EXISTS (SELECT 1 FROM performances p WHERE p.admin_id = a.id);

DROP TEMPORARY TABLE _artist_seed;
