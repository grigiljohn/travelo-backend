-- Drop old table and recreate with new schema matching the spec
DROP TABLE IF EXISTS media_files CASCADE;

-- Create new media table with UUID primary key and all spec fields
CREATE TABLE media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    media_type VARCHAR(16) NOT NULL,
    mime_type VARCHAR(128),
    filename TEXT,
    size_bytes BIGINT,
    storage_key TEXT NOT NULL,
    storage_bucket TEXT NOT NULL,
    storage_etag TEXT,
    upload_id TEXT,
    state VARCHAR(32) NOT NULL DEFAULT 'upload_pending',
    safety_status VARCHAR(32) DEFAULT 'unknown',
    variants JSONB DEFAULT '[]'::jsonb,
    meta JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_media_owner ON media(owner_id);
CREATE INDEX idx_media_state ON media(state);
CREATE INDEX idx_media_safety_status ON media(safety_status);
CREATE INDEX idx_media_storage_key ON media(storage_key);

-- Add comment on table
COMMENT ON TABLE media IS 'Media files with upload state, variants, and processing metadata';
COMMENT ON COLUMN media.variants IS 'JSON array of processed variants: [{name, key, mime, width, height, bitrate, duration}]';
COMMENT ON COLUMN media.meta IS 'JSON object with metadata: {exif, duration, width, height, codec, moderation_scores, etc.}';

