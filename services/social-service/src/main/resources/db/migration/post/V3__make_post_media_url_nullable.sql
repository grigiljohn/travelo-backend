-- Optional media_id on post_media_items; url nullable when table exists (e.g. after V2 or V1).

DO $$
BEGIN
  IF to_regclass('public.post_media_items') IS NULL THEN
    RETURN;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'post_media_items'
      AND column_name = 'media_id'
  ) THEN
    ALTER TABLE post_media_items ADD COLUMN media_id UUID;
    CREATE INDEX IF NOT EXISTS idx_post_media_items_media_id ON post_media_items(media_id);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'post_media_items'
      AND column_name = 'url' AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE post_media_items ALTER COLUMN url DROP NOT NULL;
  END IF;
END $$;
