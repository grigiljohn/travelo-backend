-- Run this on your post-service PostgreSQL database to fix 409 on text post create.
-- Cause: posts_mood_check was created by Hibernate with an old enum list that excludes NEUTRAL.
--
-- Usage (replace DB name/host as needed):
--   psql -h localhost -U postgres -d post_service -f fix_posts_mood_check.sql
-- Or run the two statements below in your SQL client (DBeaver, pgAdmin, etc.).

ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_mood_check;

ALTER TABLE posts ADD CONSTRAINT posts_mood_check CHECK (mood IN (
  'CHILL', 'LOVE', 'ADVENTURE', 'PARTY', 'NATURE', 'FOOD', 'CULTURE',
  'ROMANTIC', 'ACTIVITY', 'RELAX', 'NEUTRAL', 'HAPPY', 'EXCITED', 'CALM',
  'INSPIRED', 'GRATEFUL', 'THOUGHTFUL', 'MOTIVATED'
));
