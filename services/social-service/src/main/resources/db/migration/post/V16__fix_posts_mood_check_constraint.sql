-- Fix posts_mood_check: allow all MoodType enum values (including NEUTRAL).
-- Hibernate may have created the constraint with an older subset of values.
DO $$
BEGIN
  IF to_regclass('public.posts') IS NOT NULL THEN
    ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_mood_check;

    ALTER TABLE posts ADD CONSTRAINT posts_mood_check CHECK (mood IN (
      'CHILL', 'LOVE', 'ADVENTURE', 'PARTY', 'NATURE', 'FOOD', 'CULTURE',
      'ROMANTIC', 'ACTIVITY', 'RELAX', 'NEUTRAL', 'HAPPY', 'EXCITED', 'CALM',
      'INSPIRED', 'GRATEFUL', 'THOUGHTFUL', 'MOTIVATED'
    ));
  END IF;
END $$;
