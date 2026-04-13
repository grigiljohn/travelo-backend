-- Primary image for story-service demo story f0f39270-... (Cappadocia / balloon sky URL).
-- Stories 4f2f9eb9 / 9e031bc7 reuse post demo media f1111111-...101 / 102 (same Unsplash URLs).
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
        'f1111111-1111-4111-8111-111111111105'::uuid,
        '11111111-1111-4111-8111-111111111111'::uuid,
        'IMAGE',
        'image/jpeg',
        'original.jpg',
        269,
        'demo-seed/f1111111-1111-4111-8111-111111111105/original.jpg',
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
