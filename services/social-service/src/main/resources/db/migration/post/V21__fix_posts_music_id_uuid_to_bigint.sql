-- Earlier V19 revisions added posts.music_id as UUID by mistake. Music.id is BIGINT.
-- Hibernate generates: left join music m1_0 on m1_0.id = p1_0.music_id
-- which fails with: operator does not exist: bigint = uuid

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'posts'
      AND column_name = 'music_id'
      AND udt_name = 'uuid'
  ) THEN
    DROP INDEX IF EXISTS idx_posts_music_id;
    ALTER TABLE posts DROP COLUMN music_id;
    ALTER TABLE posts ADD COLUMN music_id BIGINT;
    CREATE INDEX IF NOT EXISTS idx_posts_music_id ON posts(music_id);
  END IF;
END $$;
