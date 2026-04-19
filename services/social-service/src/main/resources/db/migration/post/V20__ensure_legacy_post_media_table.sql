-- Defensive migration for legacy relation still referenced by Post entity / DTO mapping.
-- Some environments removed post_media but code still reads post.getMedia().

CREATE TABLE IF NOT EXISTS post_media (
    id BIGSERIAL PRIMARY KEY,
    post_id VARCHAR(50) NOT NULL,
    media_id UUID,
    media_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    media_type VARCHAR(20) NOT NULL,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_post_media_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_media_post_id ON post_media(post_id);
CREATE INDEX IF NOT EXISTS idx_post_media_order_index ON post_media(post_id, order_index);
