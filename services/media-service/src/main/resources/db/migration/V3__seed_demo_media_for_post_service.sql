-- Dev/demo media rows aligned with post-service feed seeds (data.sql + V17).
-- Physical files are written at startup by DemoLocalMediaBlobSeeder (see media.dev.seed-demo-files).
-- Storage keys are stable paths under the local base-path (not date-based raw/YYYY/MM/DD).

INSERT INTO media (
    id,
    owner_id,
    media_type,
    mime_type,
    filename,
    size_bytes,
    storage_key,
    storage_bucket,
    state,
    safety_status,
    variants,
    meta,
    created_at,
    updated_at
)
VALUES
    (
        'f1111111-1111-4111-8111-111111111101'::uuid,
        '11111111-1111-4111-8111-111111111111'::uuid,
        'IMAGE',
        'image/jpeg',
        'original.jpg',
        269,
        'demo-seed/f1111111-1111-4111-8111-111111111101/original.jpg',
        'local',
        'READY',
        'unknown',
        '[]'::jsonb,
        '{}'::jsonb,
        NOW(),
        NOW()
    ),
    (
        'f1111111-1111-4111-8111-111111111102'::uuid,
        '11111111-1111-4111-8111-111111111111'::uuid,
        'IMAGE',
        'image/jpeg',
        'original.jpg',
        631,
        'demo-seed/f1111111-1111-4111-8111-111111111102/original.jpg',
        'local',
        'READY',
        'unknown',
        '[]'::jsonb,
        '{}'::jsonb,
        NOW(),
        NOW()
    ),
    (
        'f1111111-1111-4111-8111-111111111103'::uuid,
        '11111111-1111-4111-8111-111111111111'::uuid,
        'IMAGE',
        'image/jpeg',
        'original.jpg',
        269,
        'demo-seed/f1111111-1111-4111-8111-111111111103/original.jpg',
        'local',
        'READY',
        'unknown',
        '[]'::jsonb,
        '{}'::jsonb,
        NOW(),
        NOW()
    ),
    (
        'f1111111-1111-4111-8111-111111111104'::uuid,
        '11111111-1111-4111-8111-111111111111'::uuid,
        'VIDEO',
        'video/mp4',
        'demo-video.mp4',
        788493,
        'demo-seed/f1111111-1111-4111-8111-111111111104/original.mp4',
        'local',
        'READY',
        'unknown',
        '[]'::jsonb,
        '{}'::jsonb,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO UPDATE SET
    owner_id = EXCLUDED.owner_id,
    media_type = EXCLUDED.media_type,
    mime_type = EXCLUDED.mime_type,
    filename = EXCLUDED.filename,
    size_bytes = EXCLUDED.size_bytes,
    storage_key = EXCLUDED.storage_key,
    storage_bucket = EXCLUDED.storage_bucket,
    state = EXCLUDED.state,
    safety_status = EXCLUDED.safety_status,
    variants = EXCLUDED.variants,
    meta = EXCLUDED.meta,
    updated_at = NOW();
