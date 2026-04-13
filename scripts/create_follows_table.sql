-- ============================================
-- Create Follows Table for User Follow Relationships
-- Database: travelo_auth
-- ============================================
-- 
-- This script creates the follows table for tracking user follow relationships.
-- 
-- Usage:
--   1. Connect to PostgreSQL: psql -U travelo -d travelo_auth
--   2. Run this script: \i create_follows_table.sql
--   3. Or copy-paste the SQL statements directly
--
-- ============================================

-- Create follows table for user follow relationships
CREATE TABLE IF NOT EXISTS follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_followee FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_follow UNIQUE (follower_id, followee_id),
    CONSTRAINT check_not_self_follow CHECK (follower_id != followee_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_follower_id ON follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_followee_id ON follows(followee_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_follower_followee ON follows(follower_id, followee_id);

-- Verify table was created
SELECT 'Follows table created successfully!' AS status;
