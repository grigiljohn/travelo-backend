ALTER TABLE stories ADD COLUMN IF NOT EXISTS user_name VARCHAR(120);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS user_avatar VARCHAR(500);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS video_url VARCHAR(500);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS image_urls_json TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS best_time VARCHAR(120);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS insight VARCHAR(500);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS story_type VARCHAR(20);

INSERT INTO stories (
    id,
    user_id,
    user_name,
    user_avatar,
    media_url,
    thumbnail_url,
    media_type,
    image_urls_json,
    caption,
    location,
    best_time,
    insight,
    story_type,
    music_track,
    view_count,
    reply_count,
    created_at,
    expires_at,
    is_highlight
)
SELECT *
FROM (
    VALUES
        (
            '4f2f9eb9-2a27-4fcb-8cab-d235a58e905a'::uuid,
            'u_sienna',
            'Sienna Rao',
            'https://i.pravatar.cc/200?img=32',
            'f1111111-1111-4111-8111-111111111101'::uuid,
            'https://images.unsplash.com/photo-1507525428034-b723cf961d3e',
            'https://images.unsplash.com/photo-1507525428034-b723cf961d3e',
            'IMAGE',
            '["https://images.unsplash.com/photo-1507525428034-b723cf961d3e","https://images.unsplash.com/photo-1483683804023-6ccdb62f86ef"]',
            'Maldives sunrise looked unreal this morning.',
            'Maldives',
            'Sunrise',
            'Go before 7:00 AM for softer light and fewer boats.',
            'place',
            'Ocean Drift - Lune',
            12,
            3,
            NOW() - INTERVAL '1 hour',
            NOW() + INTERVAL '23 hours',
            FALSE
        ),
        (
            '9e031bc7-91f4-4a7f-9bf5-a31f8938bf67'::uuid,
            'u_noah',
            'Noah Klein',
            'https://i.pravatar.cc/200?img=12',
            'https://images.unsplash.com/photo-1469474968028-56623f02e42e',
            'https://images.unsplash.com/photo-1469474968028-56623f02e42e',
            'IMAGE',
            '["https://images.unsplash.com/photo-1469474968028-56623f02e42e"]',
            'Swiss train route was peak cinematic.',
            'Interlaken',
            'Golden hour',
            'Sit on the right side from Zurich to catch valley views.',
            'trip',
            'Northbound - Aria',
            21,
            5,
            NOW() - INTERVAL '2 hour',
            NOW() + INTERVAL '22 hours',
            FALSE
        ),
        (
            'f0f39270-c24c-41d3-a26a-2f43fd331047'::uuid,
            'u_ava',
            'Ava Park',
            'https://i.pravatar.cc/200?img=45',
            'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1',
            'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1',
            'IMAGE',
            '["https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1","https://images.unsplash.com/photo-1470770841072-f978cf4d019e"]',
            'A tiny memory from Cappadocia skies.',
            'Cappadocia',
            'Early morning',
            'Book balloons one day early in case weather cancels.',
            'memory',
            'Float - Ember',
            8,
            1,
            NOW() - INTERVAL '30 minutes',
            NOW() + INTERVAL '23 hours 30 minutes',
            FALSE
        )
) AS seed_data (
    id,
    user_id,
    user_name,
    user_avatar,
    media_url,
    thumbnail_url,
    media_type,
    image_urls_json,
    caption,
    location,
    best_time,
    insight,
    story_type,
    music_track,
    view_count,
    reply_count,
    created_at,
    expires_at,
    is_highlight
)
WHERE NOT EXISTS (SELECT 1 FROM stories);
