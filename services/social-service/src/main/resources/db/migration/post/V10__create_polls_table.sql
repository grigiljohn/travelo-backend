-- V10: Create polls table for poll creation
CREATE TABLE IF NOT EXISTS polls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID,
    question TEXT NOT NULL,
    options JSONB NOT NULL, -- Array of option strings
    total_votes INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_polls_post_id ON polls(post_id);
CREATE INDEX IF NOT EXISTS idx_polls_expires_at ON polls(expires_at) WHERE expires_at IS NOT NULL;

-- Create poll_votes table
CREATE TABLE IF NOT EXISTS poll_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    option_index INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(poll_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_poll_votes_poll_id ON poll_votes(poll_id);
CREATE INDEX IF NOT EXISTS idx_poll_votes_user_id ON poll_votes(user_id);

DO $$
BEGIN
  ALTER TABLE polls OWNER TO travelo;
  ALTER TABLE poll_votes OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE polls TO travelo;
  GRANT ALL PRIVILEGES ON TABLE poll_votes TO travelo;
  GRANT ALL PRIVILEGES ON TABLE polls TO PUBLIC;
  GRANT ALL PRIVILEGES ON TABLE poll_votes TO PUBLIC;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

