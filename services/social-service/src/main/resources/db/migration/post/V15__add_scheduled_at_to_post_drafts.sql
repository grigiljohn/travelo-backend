-- Add scheduled_at column to post_drafts table
ALTER TABLE post_drafts ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP WITH TIME ZONE;

-- Create index for scheduled drafts
CREATE INDEX IF NOT EXISTS idx_post_drafts_scheduled_at ON post_drafts(scheduled_at) WHERE scheduled_at IS NOT NULL;

