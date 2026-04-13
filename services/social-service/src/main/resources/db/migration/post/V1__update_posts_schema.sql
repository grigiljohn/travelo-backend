-- Migration script to update posts table to new schema (safe on empty DB)

-- Step 0: Greenfield — posts must exist before ALTERs / child tables
CREATE TABLE IF NOT EXISTS posts (
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
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE,
  deleted_at TIMESTAMP WITH TIME ZONE
);

-- Step 2: Drop legacy FKs only when those tables exist
DO $$
BEGIN
  IF to_regclass('public.post_media') IS NOT NULL THEN
    ALTER TABLE post_media DROP CONSTRAINT IF EXISTS post_media_post_id_fkey;
  END IF;
  IF to_regclass('public.music') IS NOT NULL THEN
    ALTER TABLE music DROP CONSTRAINT IF EXISTS posts_music_id_fkey;
  END IF;
END $$;

-- Step 3: Add new columns if they don't exist
ALTER TABLE posts 
  ADD COLUMN IF NOT EXISTS user_id VARCHAR(50),
  ADD COLUMN IF NOT EXISTS post_type VARCHAR(20),
  ADD COLUMN IF NOT EXISTS mood VARCHAR(20),
  ADD COLUMN IF NOT EXISTS caption VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS location VARCHAR(255),
  ADD COLUMN IF NOT EXISTS likes INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS comments INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS remixes INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS tips INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS shares INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS duration INTEGER,
  ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500),
  ADD COLUMN IF NOT EXISTS music_track VARCHAR(255),
  ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- Step 4: Update data for existing rows (if any)
-- Convert author_id (UUID) to user_id (VARCHAR) if needed
-- UPDATE posts SET user_id = author_id::VARCHAR WHERE author_id IS NOT NULL AND user_id IS NULL;

-- Step 5: Set NOT NULL constraints after data migration
ALTER TABLE posts 
  ALTER COLUMN user_id SET NOT NULL,
  ALTER COLUMN post_type SET NOT NULL,
  ALTER COLUMN mood SET NOT NULL,
  ALTER COLUMN likes SET DEFAULT 0,
  ALTER COLUMN comments SET DEFAULT 0,
  ALTER COLUMN remixes SET DEFAULT 0,
  ALTER COLUMN tips SET DEFAULT 0,
  ALTER COLUMN shares SET DEFAULT 0;

-- Step 6: Create indexes
CREATE INDEX IF NOT EXISTS idx_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_mood ON posts(mood);
CREATE INDEX IF NOT EXISTS idx_created_at ON posts(created_at);
CREATE INDEX IF NOT EXISTS idx_post_type ON posts(post_type);
CREATE INDEX IF NOT EXISTS idx_deleted_at ON posts(deleted_at);

-- Step 7: Change id column from BIGINT to VARCHAR(50) if needed
-- WARNING: This will lose all existing data!
-- For production, you need a more careful migration strategy
-- For development, you can drop and recreate:
-- DROP TABLE IF EXISTS posts CASCADE;
-- (Then let Hibernate recreate it with ddl-auto: create)

-- Step 8: Create post_media_items table if it doesn't exist
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
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_media_items_post_id ON post_media_items(post_id);
CREATE INDEX IF NOT EXISTS idx_post_media_items_position ON post_media_items(post_id, position);

-- Step 9: Create post_tags table if it doesn't exist
CREATE TABLE IF NOT EXISTS post_tags (
  id BIGSERIAL PRIMARY KEY,
  post_id VARCHAR(50) NOT NULL,
  tag VARCHAR(100) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_tags_post_id ON post_tags(post_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag ON post_tags(tag);

-- Step 10: Create likes table if it doesn't exist
CREATE TABLE IF NOT EXISTS likes (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  post_id VARCHAR(50) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_likes_post_id ON likes(post_id);
CREATE INDEX IF NOT EXISTS idx_likes_user_id ON likes(user_id);

