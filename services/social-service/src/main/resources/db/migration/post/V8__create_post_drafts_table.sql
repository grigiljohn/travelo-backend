-- Create post_drafts table for storing unpublished post drafts
CREATE TABLE IF NOT EXISTS post_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    media_file_path VARCHAR(500),
    is_video BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(255),
    caption VARCHAR(2000),
    text VARCHAR(2000),
    hashtags VARCHAR(1000),
    location VARCHAR(255),
    tagged_users VARCHAR(1000),
    audience VARCHAR(50),
    allow_comments BOOLEAN NOT NULL DEFAULT TRUE,
    hide_likes_count BOOLEAN NOT NULL DEFAULT FALSE,
    allow_remixing BOOLEAN NOT NULL DEFAULT TRUE,
    ai_label_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    music_track_id VARCHAR(255),
    filter VARCHAR(100),
    create_mode VARCHAR(50),
    cover_image_path VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_post_drafts_user_id ON post_drafts(user_id);
CREATE INDEX IF NOT EXISTS idx_post_drafts_created_at ON post_drafts(created_at);
CREATE INDEX IF NOT EXISTS idx_post_drafts_updated_at ON post_drafts(updated_at);

DO $$
BEGIN
  ALTER TABLE post_drafts OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE post_drafts TO travelo;
  GRANT ALL PRIVILEGES ON TABLE post_drafts TO PUBLIC;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

