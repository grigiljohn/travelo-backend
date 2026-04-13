-- Flyway migration for Story Service tables

-- Stories table (ephemeral content with 24h TTL)
CREATE TABLE IF NOT EXISTS stories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    media_id UUID,
    media_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    media_type VARCHAR(20) NOT NULL,
    caption VARCHAR(500),
    location VARCHAR(255),
    music_track VARCHAR(255),
    view_count INTEGER NOT NULL DEFAULT 0,
    reply_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_highlight BOOLEAN NOT NULL DEFAULT FALSE,
    highlight_id UUID,
    CONSTRAINT chk_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))
);

-- Indexes for stories
CREATE INDEX IF NOT EXISTS idx_story_user_id ON stories(user_id);
CREATE INDEX IF NOT EXISTS idx_story_created_at ON stories(created_at);
CREATE INDEX IF NOT EXISTS idx_story_expires_at ON stories(expires_at);

-- Story views table (tracks who viewed which story)
CREATE TABLE IF NOT EXISTS story_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    viewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_story_view_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    CONSTRAINT uk_story_view_unique UNIQUE (story_id, user_id)
);

-- Indexes for story_views
CREATE INDEX IF NOT EXISTS idx_story_view_story_id ON story_views(story_id);
CREATE INDEX IF NOT EXISTS idx_story_view_user_id ON story_views(user_id);

-- Story replies table
CREATE TABLE IF NOT EXISTS story_replies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    reply_text VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_story_reply_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

-- Set ownership of story_replies table
ALTER TABLE IF EXISTS story_replies OWNER TO travelo;

-- Indexes for story_replies
CREATE INDEX IF NOT EXISTS idx_story_reply_story_id ON story_replies(story_id);
CREATE INDEX IF NOT EXISTS idx_story_reply_user_id ON story_replies(user_id);
CREATE INDEX IF NOT EXISTS idx_story_reply_created_at ON story_replies(created_at);

-- Story highlights table (permanent collections)
CREATE TABLE IF NOT EXISTS story_highlights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    cover_image_url VARCHAR(500),
    story_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for story_highlights
CREATE INDEX IF NOT EXISTS idx_story_highlight_user_id ON story_highlights(user_id);
CREATE INDEX IF NOT EXISTS idx_story_highlight_created_at ON story_highlights(created_at);

-- Function to automatically delete expired stories (optional, can be done via scheduled job)
CREATE OR REPLACE FUNCTION delete_expired_stories()
RETURNS void AS $$
BEGIN
    DELETE FROM stories WHERE expires_at < NOW() AND is_highlight = FALSE;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  ALTER TABLE IF EXISTS stories OWNER TO travelo;
  ALTER TABLE IF EXISTS story_views OWNER TO travelo;
  ALTER TABLE IF EXISTS story_replies OWNER TO travelo;
  ALTER TABLE IF EXISTS story_highlights OWNER TO travelo;
  ALTER FUNCTION delete_expired_stories() OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE stories TO travelo;
  GRANT ALL PRIVILEGES ON TABLE story_views TO travelo;
  GRANT ALL PRIVILEGES ON TABLE story_replies TO travelo;
  GRANT ALL PRIVILEGES ON TABLE story_highlights TO travelo;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

