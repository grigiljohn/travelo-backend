-- Flyway migration for Reel Service tables

-- Reels table (short video content)
CREATE TABLE IF NOT EXISTS reels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id VARCHAR(50) UNIQUE, -- Reference to post-service post
    user_id VARCHAR(50) NOT NULL,
    media_id UUID, -- Reference to media-service media ID
    video_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    caption VARCHAR(1000),
    music_track VARCHAR(255),
    location VARCHAR(255),
    duration_seconds INTEGER,
    like_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    share_count INTEGER NOT NULL DEFAULT 0,
    view_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transcoding_status VARCHAR(20) DEFAULT 'PENDING',
    ranking_score DOUBLE PRECISION,
    recommendation_score DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_reel_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_reel_transcoding_status CHECK (transcoding_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

-- Indexes for reels
CREATE INDEX IF NOT EXISTS idx_reel_user_id ON reels(user_id);
CREATE INDEX IF NOT EXISTS idx_reel_created_at ON reels(created_at);
CREATE INDEX IF NOT EXISTS idx_reel_post_id ON reels(post_id);
CREATE INDEX IF NOT EXISTS idx_reel_status ON reels(status);
CREATE INDEX IF NOT EXISTS idx_reel_ranking_score ON reels(ranking_score DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_reel_recommendation_score ON reels(recommendation_score DESC NULLS LAST);

-- Reel likes table
CREATE TABLE IF NOT EXISTS reel_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    liked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reel_like_reel FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,
    CONSTRAINT uk_reel_like_unique UNIQUE (reel_id, user_id)
);

-- Indexes for reel_likes
CREATE INDEX IF NOT EXISTS idx_reel_like_reel_id ON reel_likes(reel_id);
CREATE INDEX IF NOT EXISTS idx_reel_like_user_id ON reel_likes(user_id);

-- Reel comments table
CREATE TABLE IF NOT EXISTS reel_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    comment_text VARCHAR(1000) NOT NULL,
    parent_id UUID, -- For threaded comments
    like_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reel_comment_reel FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE,
    CONSTRAINT fk_reel_comment_parent FOREIGN KEY (parent_id) REFERENCES reel_comments(id) ON DELETE CASCADE
);

-- Indexes for reel_comments
CREATE INDEX IF NOT EXISTS idx_reel_comment_reel_id ON reel_comments(reel_id);
CREATE INDEX IF NOT EXISTS idx_reel_comment_user_id ON reel_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_reel_comment_created_at ON reel_comments(created_at);
CREATE INDEX IF NOT EXISTS idx_reel_comment_parent_id ON reel_comments(parent_id);

-- Reel views table (for analytics)
CREATE TABLE IF NOT EXISTS reel_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    view_duration_seconds INTEGER,
    completion_percentage DOUBLE PRECISION,
    viewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reel_view_reel FOREIGN KEY (reel_id) REFERENCES reels(id) ON DELETE CASCADE
);

-- Indexes for reel_views
CREATE INDEX IF NOT EXISTS idx_reel_view_reel_id ON reel_views(reel_id);
CREATE INDEX IF NOT EXISTS idx_reel_view_user_id ON reel_views(user_id);
CREATE INDEX IF NOT EXISTS idx_reel_view_created_at ON reel_views(viewed_at);

