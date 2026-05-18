-- Temporary coordinates are placeholders and should be updated to match the final festival map.
INSERT INTO map_locations (
    location_name,
    sector,
    map_x,
    map_y,
    width,
    height,
    location_type,
    display_order,
    display_status,
    created_at,
    updated_at
)
SELECT
    '언기도 앞',
    'PERF',
    0.0000,
    0.0000,
    1.000,
    1.000,
    'STAGE',
    101,
    'VISIBLE',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM map_locations
    WHERE location_name = '언기도 앞'
);

INSERT INTO map_locations (
    location_name,
    sector,
    map_x,
    map_y,
    width,
    height,
    location_type,
    display_order,
    display_status,
    created_at,
    updated_at
)
SELECT
    '노천극장',
    'PERF',
    0.0000,
    0.0000,
    1.000,
    1.000,
    'STAGE',
    102,
    'VISIBLE',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM map_locations
    WHERE location_name = '노천극장'
);

INSERT INTO map_locations (
    location_name,
    sector,
    map_x,
    map_y,
    width,
    height,
    location_type,
    display_order,
    display_status,
    created_at,
    updated_at
)
SELECT
    '동문광장',
    'PERF',
    0.0000,
    0.0000,
    1.000,
    1.000,
    'STAGE',
    103,
    'VISIBLE',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM map_locations
    WHERE location_name = '동문광장'
);
