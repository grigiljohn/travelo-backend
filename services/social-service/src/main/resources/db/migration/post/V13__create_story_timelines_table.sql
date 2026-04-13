-- V13: Create story_timelines table for AI-generated story timelines
CREATE TABLE IF NOT EXISTS story_timelines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    media_order JSONB NOT NULL, -- Array of media IDs in order
    durations JSONB NOT NULL, -- Map of media_id -> duration_seconds
    transitions JSONB, -- Array of transition configs: [{"from": 0, "to": 1, "type": "fade", "duration": 500}]
    text_overlays JSONB, -- Array of text overlay configs
    template_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_story_timelines_user_id ON story_timelines(user_id);
CREATE INDEX IF NOT EXISTS idx_story_timelines_created_at ON story_timelines(created_at DESC);

DO $$
BEGIN
  ALTER TABLE story_timelines OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE story_timelines TO travelo;
  GRANT ALL PRIVILEGES ON TABLE story_timelines TO PUBLIC;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

