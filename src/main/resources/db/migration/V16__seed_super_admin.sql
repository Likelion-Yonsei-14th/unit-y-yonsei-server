INSERT INTO admin_users (
    login_id,
    password_hash,
    organization,
    role,
    status,
    representative_name,
    representative_phone,
    last_login_at,
    memo,
    created_at,
    updated_at
)
SELECT
    'super',
    CAST(UNHEX('243261243130243675622f6b556661645574757845343635513535324f385549784a466b6e5a5841413042577442494c756274486f6d655a31525543') AS CHAR),
    '멋쟁이사자처럼',
    'SUPER',
    'ACTIVE',
    '슈퍼 관리자',
    '010-0000-0000',
    NULL,
    '최초 Super Admin, 비밀번호 초기화 필요',
    NOW(6),
    NOW(6)
    WHERE NOT EXISTS (
  SELECT 1
  FROM admin_users
  WHERE login_id = 'super'
);