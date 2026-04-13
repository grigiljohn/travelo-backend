-- Create saved_posts table for users to save posts to collections
CREATE TABLE IF NOT EXISTS saved_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    post_id VARCHAR(50) NOT NULL,
    collection_name VARCHAR(100) DEFAULT 'All Posts',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, post_id, collection_name),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_saved_posts_user_id ON saved_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_posts_post_id ON saved_posts(post_id);
CREATE INDEX IF NOT EXISTS idx_saved_posts_collection ON saved_posts(user_id, collection_name);
CREATE INDEX IF NOT EXISTS idx_saved_posts_created_at ON saved_posts(created_at);

