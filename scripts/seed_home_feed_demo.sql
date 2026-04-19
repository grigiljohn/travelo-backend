-- Seed minimal home-feed demo data (idempotent).
-- Target DB: travelo_posts (social-service post datasource).
--
-- Usage example:
--   psql -h localhost -p 5432 -U travelo -d travelo_posts -f scripts/seed_home_feed_demo.sql

BEGIN;

INSERT INTO posts (
    id,
    user_id,
    post_type,
    content,
    caption,
    mood,
    location,
    likes,
    comments,
    remixes,
    tips,
    shares,
    duration,
    thumbnail_url,
    music_track,
    is_verified,
    privacy_level,
    is_archived,
    created_at,
    updated_at,
    deleted_at
)
VALUES
    (
        'post_feed_demo_001',
        'u_sienna',
        'IMAGE',
        'Sunrise tones over the bay.',
        'Golden hour in Santorini felt unreal.',
        'INSPIRED',
        'Oia, Santorini',
        842,
        67,
        12,
        4,
        28,
        NULL,
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e',
        'Ocean Drift - Lune',
        TRUE,
        'PUBLIC',
        FALSE,
        NOW() - INTERVAL '3 hours',
        NOW() - INTERVAL '3 hours',
        NULL
    ),
    (
        'post_feed_demo_002',
        'u_noah',
        'MIXED',
        'Swiss route highlights.',
        'A 2-day rail plan through the Alps. Save this.',
        'ADVENTURE',
        'Interlaken, Switzerland',
        524,
        39,
        8,
        2,
        19,
        NULL,
        'https://images.unsplash.com/photo-1469474968028-56623f02e42e',
        'Northbound - Aria',
        FALSE,
        'PUBLIC',
        FALSE,
        NOW() - INTERVAL '8 hours',
        NOW() - INTERVAL '8 hours',
        NULL
    ),
    (
        'post_feed_demo_003',
        'u_ava',
        'VIDEO',
        'Balloon launch clip.',
        'Cappadocia dawn from the ridge line.',
        'EXCITED',
        'Cappadocia, Turkey',
        1330,
        104,
        21,
        11,
        71,
        19,
        'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1',
        'Float - Ember',
        TRUE,
        'PUBLIC',
        FALSE,
        NOW() - INTERVAL '14 hours',
        NOW() - INTERVAL '14 hours',
        NULL
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO post_media_items (
    post_id,
    url,
    type,
    position,
    thumbnail_url,
    duration,
    width,
    height,
    created_at
)
SELECT
    'post_feed_demo_001',
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e',
    'IMAGE',
    0,
    'https://images.unsplash.com/photo-1507525428034-b723cf961d3e',
    NULL,
    1200,
    1500,
    NOW() - INTERVAL '3 hours'
WHERE NOT EXISTS (
    SELECT 1 FROM post_media_items WHERE post_id = 'post_feed_demo_001' AND position = 0
);

INSERT INTO post_media_items (
    post_id,
    url,
    type,
    position,
    thumbnail_url,
    duration,
    width,
    height,
    created_at
)
SELECT
    'post_feed_demo_002',
    'https://images.unsplash.com/photo-1469474968028-56623f02e42e',
    'IMAGE',
    0,
    'https://images.unsplash.com/photo-1469474968028-56623f02e42e',
    NULL,
    1200,
    1500,
    NOW() - INTERVAL '8 hours'
WHERE NOT EXISTS (
    SELECT 1 FROM post_media_items WHERE post_id = 'post_feed_demo_002' AND position = 0
);

INSERT INTO post_media_items (
    post_id,
    url,
    type,
    position,
    thumbnail_url,
    duration,
    width,
    height,
    created_at
)
SELECT
    'post_feed_demo_003',
    'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4',
    'VIDEO',
    0,
    'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1',
    19,
    720,
    1280,
    NOW() - INTERVAL '14 hours'
WHERE NOT EXISTS (
    SELECT 1 FROM post_media_items WHERE post_id = 'post_feed_demo_003' AND position = 0
);

INSERT INTO post_tags (post_id, tag, created_at)
SELECT 'post_feed_demo_001', '#santorini', NOW() - INTERVAL '3 hours'
WHERE NOT EXISTS (
    SELECT 1 FROM post_tags WHERE post_id = 'post_feed_demo_001' AND tag = '#santorini'
);

INSERT INTO post_tags (post_id, tag, created_at)
SELECT 'post_feed_demo_002', '#switzerland', NOW() - INTERVAL '8 hours'
WHERE NOT EXISTS (
    SELECT 1 FROM post_tags WHERE post_id = 'post_feed_demo_002' AND tag = '#switzerland'
);

INSERT INTO post_tags (post_id, tag, created_at)
SELECT 'post_feed_demo_003', '#cappadocia', NOW() - INTERVAL '14 hours'
WHERE NOT EXISTS (
    SELECT 1 FROM post_tags WHERE post_id = 'post_feed_demo_003' AND tag = '#cappadocia'
);

COMMIT;

-- Quick verification
SELECT id, user_id, post_type, created_at
FROM posts
WHERE id LIKE 'post_feed_demo_%'
ORDER BY created_at DESC;
