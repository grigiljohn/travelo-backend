-- V12: Create audio_library table for audio/sound effects
CREATE TABLE IF NOT EXISTS audio_library (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    duration_seconds INTEGER,
    category VARCHAR(100), -- e.g., 'sound_effect', 'ambient', 'nature', 'urban'
    thumbnail_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_audio_library_category ON audio_library(category) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_audio_library_active ON audio_library(is_active) WHERE is_active = TRUE;

-- Create post_audio junction table
CREATE TABLE IF NOT EXISTS post_audio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    audio_id UUID NOT NULL REFERENCES audio_library(id) ON DELETE CASCADE,
    fade_in_duration INTEGER DEFAULT 0, -- milliseconds
    fade_out_duration INTEGER DEFAULT 0, -- milliseconds
    volume DECIMAL(3,2) DEFAULT 1.0, -- 0.0 to 1.0
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, audio_id)
);

CREATE INDEX IF NOT EXISTS idx_post_audio_post_id ON post_audio(post_id);
CREATE INDEX IF NOT EXISTS idx_post_audio_audio_id ON post_audio(audio_id);

DO $$
BEGIN
  ALTER TABLE audio_library OWNER TO travelo;
  ALTER TABLE post_audio OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE audio_library TO travelo;
  GRANT ALL PRIVILEGES ON TABLE post_audio TO travelo;
  GRANT ALL PRIVILEGES ON TABLE audio_library TO PUBLIC;
  GRANT ALL PRIVILEGES ON TABLE post_audio TO PUBLIC;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

