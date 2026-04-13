-- Create post_comments table for post comments with threaded replies support
CREATE TABLE IF NOT EXISTS post_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    comment_text VARCHAR(2000) NOT NULL,
    parent_id UUID, -- For threaded replies
    like_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES post_comments(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_post_comment_post_id ON post_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_post_comment_user_id ON post_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_post_comment_created_at ON post_comments(created_at);
CREATE INDEX IF NOT EXISTS idx_post_comment_parent_id ON post_comments(parent_id);
CREATE INDEX IF NOT EXISTS idx_post_comment_deleted_at ON post_comments(deleted_at) WHERE deleted_at IS NULL;

