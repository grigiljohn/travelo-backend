-- Add privacy and archive fields to posts table.
-- Repair: some DBs had an older V2 that dropped posts without recreating; V3 can no-op and leave no posts row.
DO $$
BEGIN
  IF to_regclass('public.posts') IS NULL THEN
    CREATE TABLE posts (
      id VARCHAR(50) PRIMARY KEY,
      user_id VARCHAR(50) NOT NULL,
      post_type VARCHAR(20) NOT NULL,
      content VARCHAR(500),
      caption VARCHAR(1000),
      mood VARCHAR(20) NOT NULL,
      location VARCHAR(255),
      likes INTEGER NOT NULL DEFAULT 0,
      comments INTEGER NOT NULL DEFAULT 0,
      remixes INTEGER NOT NULL DEFAULT 0,
      tips INTEGER NOT NULL DEFAULT 0,
      shares INTEGER NOT NULL DEFAULT 0,
      duration INTEGER,
      thumbnail_url VARCHAR(500),
      music_track VARCHAR(255),
      is_verified BOOLEAN DEFAULT FALSE,
      privacy_level VARCHAR(20) DEFAULT 'PUBLIC',
      is_archived BOOLEAN DEFAULT FALSE,
      archived_at TIMESTAMP WITH TIME ZONE,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMP WITH TIME ZONE,
      deleted_at TIMESTAMP WITH TIME ZONE
    );
    CREATE INDEX IF NOT EXISTS idx_user_id ON posts(user_id);
    CREATE INDEX IF NOT EXISTS idx_mood ON posts(mood);
    CREATE INDEX IF NOT EXISTS idx_created_at ON posts(created_at);
    CREATE INDEX IF NOT EXISTS idx_post_type ON posts(post_type);
    CREATE INDEX IF NOT EXISTS idx_deleted_at ON posts(deleted_at);
  ELSE
    ALTER TABLE posts
      ADD COLUMN IF NOT EXISTS privacy_level VARCHAR(20) DEFAULT 'PUBLIC',
      ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT FALSE,
      ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP WITH TIME ZONE;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_posts_privacy_level ON posts(privacy_level);
CREATE INDEX IF NOT EXISTS idx_posts_is_archived ON posts(is_archived) WHERE is_archived = FALSE;
