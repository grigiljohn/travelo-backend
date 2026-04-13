-- V9: Create topics table for trending topics
CREATE TABLE IF NOT EXISTS topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    trend_score DECIMAL(10,2) DEFAULT 0,
    post_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_topics_trend_score ON topics(trend_score DESC) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_topics_name ON topics(name);
CREATE INDEX IF NOT EXISTS idx_topics_active ON topics(is_active) WHERE is_active = TRUE;

-- Create junction table for post-topic associations
CREATE TABLE IF NOT EXISTS post_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, topic_id)
);

CREATE INDEX IF NOT EXISTS idx_post_topics_post_id ON post_topics(post_id);
CREATE INDEX IF NOT EXISTS idx_post_topics_topic_id ON post_topics(topic_id);

DO $$
BEGIN
  ALTER TABLE topics OWNER TO travelo;
  ALTER TABLE post_topics OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE topics TO travelo;
  GRANT ALL PRIVILEGES ON TABLE post_topics TO travelo;
  GRANT ALL PRIVILEGES ON TABLE topics TO PUBLIC;
  GRANT ALL PRIVILEGES ON TABLE post_topics TO PUBLIC;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

