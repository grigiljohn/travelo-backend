CREATE TABLE IF NOT EXISTS media_files (
    id BIGSERIAL PRIMARY KEY,
    file_key VARCHAR(512) NOT NULL UNIQUE,
    file_url VARCHAR(1024) NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    moderation_reason VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_media_files_status ON media_files (status);

