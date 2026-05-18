-- 부스 프로필 선택 필드 nullable 전환
-- 부스 어드민이 최초 등록 시 필수 항목(name, status, is_food, is_reservable)만 입력하고
-- 나머지 프로필 정보는 나중에 단계적으로 채울 수 있도록 NULL 허용.
ALTER TABLE booths
    MODIFY COLUMN organization VARCHAR(100)  NULL,
    MODIFY COLUMN date         INT           NULL,
    MODIFY COLUMN open_time    TIME          NULL,
    MODIFY COLUMN close_time   TIME          NULL,
    MODIFY COLUMN sector       VARCHAR(10)   NULL,
    MODIFY COLUMN location     INT           NULL;
