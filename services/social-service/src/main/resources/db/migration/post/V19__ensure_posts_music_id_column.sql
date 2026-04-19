-- Defensive migration for schema drift:
-- Post entity still maps optional legacy relation via posts.music_id -> Music.id.
-- Type must be BIGINT to match music.id (see Music entity); UUID breaks the join at runtime.

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS music_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_posts_music_id ON posts(music_id);
