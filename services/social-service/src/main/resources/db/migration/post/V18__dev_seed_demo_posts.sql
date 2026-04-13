-- Dev/demo seed data for post-service feed testing.
-- Safe to rerun: uses ON CONFLICT / NOT EXISTS guards.

-- Repair: child tables missing when posts was recreated in V5 without V2.
CREATE TABLE IF NOT EXISTS post_media_items (
  id BIGSERIAL PRIMARY KEY,
  post_id VARCHAR(50) NOT NULL,
  url VARCHAR(500) NOT NULL,
  type VARCHAR(20) NOT NULL,
  position INTEGER NOT NULL,
  thumbnail_url VARCHAR(500),
  duration INTEGER,
  width INTEGER,
  height INTEGER,
  media_id UUID,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_post_media_items_post_id ON post_media_items(post_id);
CREATE INDEX IF NOT EXISTS idx_post_media_items_position ON post_media_items(post_id, position);

CREATE TABLE IF NOT EXISTS post_tags (
  id BIGSERIAL PRIMARY KEY,
  post_id VARCHAR(50) NOT NULL,
  tag VARCHAR(100) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_post_tags_post_id ON post_tags(post_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag ON post_tags(tag);

DO $$
BEGIN
  IF to_regclass('public.post_media_items') IS NOT NULL
     AND NOT EXISTS (
       SELECT 1 FROM information_schema.columns
       WHERE table_schema = 'public' AND table_name = 'post_media_items' AND column_name = 'media_id'
     ) THEN
    ALTER TABLE post_media_items ADD COLUMN media_id UUID;
    CREATE INDEX IF NOT EXISTS idx_post_media_items_media_id ON post_media_items(media_id);
  END IF;
END $$;

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
    media_id,
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
    'f1111111-1111-4111-8111-111111111101'::uuid,
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
    media_id,
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
    NULL,
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
    media_id,
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
    'f1111111-1111-4111-8111-111111111103'::uuid,
    'https://images.unsplash.com/photo-1470770841072-f978cf4d019e',
    'IMAGE',
    1,
    'https://images.unsplash.com/photo-1470770841072-f978cf4d019e',
    NULL,
    1200,
    1500,
    NOW() - INTERVAL '8 hours'
WHERE NOT EXISTS (
    SELECT 1 FROM post_media_items WHERE post_id = 'post_feed_demo_002' AND position = 1
);

INSERT INTO post_media_items (
    post_id,
    media_id,
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
    'f1111111-1111-4111-8111-111111111104'::uuid,
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
-- Default templates if missing (id + timestamps for legacy tables without column DEFAULTs)
INSERT INTO post_templates (id, template_id, label, icon, is_default, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), 'revert', 'Revert', 'close', FALSE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM post_templates WHERE template_id = 'revert');

INSERT INTO post_templates (id, template_id, label, icon, is_default, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), 'edit', 'Embrace l...', 'edit', TRUE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM post_templates WHERE template_id = 'edit');

INSERT INTO post_templates (id, template_id, label, icon, is_default, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), 'share', 'Share wo...', 'image', FALSE, TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM post_templates WHERE template_id = 'share');

INSERT INTO post_templates (id, template_id, label, icon, is_default, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), 'enjoy', 'Enjoy you...', 'image', FALSE, TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM post_templates WHERE template_id = 'enjoy');

INSERT INTO post_templates (id, template_id, label, icon, is_default, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), 'continue', 'continue...', 'image', FALSE, TRUE, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM post_templates WHERE template_id = 'continue');

INSERT INTO post_templates (id, template_id, label, icon, is_default, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), 'origin', 'Origin', 'image', FALSE, TRUE, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM post_templates WHERE template_id = 'origin');

