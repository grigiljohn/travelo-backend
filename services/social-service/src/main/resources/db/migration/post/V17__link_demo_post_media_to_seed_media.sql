-- Align feed demo post_media_items with media-service V3 demo rows (stable UUIDs).
-- No-op when post_media_items or media_id is missing. EXECUTE defers SQL planning so
-- missing tables do not fail migration (static UPDATE inside plpgsql is still validated).
DO $$
BEGIN
  IF to_regclass('public.post_media_items') IS NOT NULL
     AND EXISTS (
       SELECT 1 FROM information_schema.columns
       WHERE table_schema = 'public' AND table_name = 'post_media_items'
         AND column_name = 'media_id'
     ) THEN
    EXECUTE 'UPDATE post_media_items SET media_id = $1 WHERE post_id = $2 AND position = $3'
      USING 'f1111111-1111-4111-8111-111111111101'::uuid, 'post_feed_demo_001', 0;
    EXECUTE 'UPDATE post_media_items SET media_id = $1 WHERE post_id = $2 AND position = $3'
      USING 'f1111111-1111-4111-8111-111111111102'::uuid, 'post_feed_demo_002', 0;
    EXECUTE 'UPDATE post_media_items SET media_id = $1 WHERE post_id = $2 AND position = $3'
      USING 'f1111111-1111-4111-8111-111111111103'::uuid, 'post_feed_demo_002', 1;
    EXECUTE 'UPDATE post_media_items SET media_id = $1 WHERE post_id = $2 AND position = $3'
      USING 'f1111111-1111-4111-8111-111111111104'::uuid, 'post_feed_demo_003', 0;
  END IF;
END $$;
