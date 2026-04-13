-- Music catalog (merged from music-service). Co-located in travelo_media DB with media files.
CREATE TABLE IF NOT EXISTS music_tracks (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    artist VARCHAR(200) NOT NULL,
    mood VARCHAR(50),
    duration_seconds INTEGER,
    file_key VARCHAR(500) NOT NULL,
    thumbnail_key VARCHAR(500),
    thumbnail_url TEXT,
    file_url TEXT,
    is_recommended BOOLEAN DEFAULT FALSE,
    play_count BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_music_tracks_mood ON music_tracks(mood) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_music_tracks_recommended ON music_tracks(is_recommended) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_music_tracks_active ON music_tracks(is_active);
CREATE INDEX IF NOT EXISTS idx_music_tracks_name_artist ON music_tracks USING gin(to_tsvector('english', name || ' ' || artist));
