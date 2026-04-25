-- Extra library metadata (admin / discovery)
ALTER TABLE music_tracks
    ADD COLUMN IF NOT EXISTS genre VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bpm INTEGER,
    ADD COLUMN IF NOT EXISTS description TEXT;

CREATE INDEX IF NOT EXISTS idx_music_tracks_genre ON music_tracks(genre) WHERE is_active = TRUE;
