-- DEVELOPMENT: wipe post domain tables and recreate (Flyway owns schema; Hibernate ddl-auto is none).

DROP TABLE IF EXISTS likes CASCADE;
DROP TABLE IF EXISTS post_tags CASCADE;
DROP TABLE IF EXISTS post_media_items CASCADE;
DROP TABLE IF EXISTS post_media CASCADE;
DROP TABLE IF EXISTS posts CASCADE;

CREATE TABLE posts (
  id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  post_type VARCHAR(20) NOT NULL,
  content VARCHAR(500),
  caption VARCHAR(1000),
  mood VARCHAR(20) NOT NULL,
  location VARCHAR(255),
  likes INTEGER NOT NULL DEFAULT 0,
  comments INTEGER NOT NULL DEFAULT 0,
  remixes INTEGER NOT NULL DEFAULT 0,
  tips INTEGER NOT NULL DEFAULT 0,
  shares INTEGER NOT NULL DEFAULT 0,
  duration INTEGER,
  thumbnail_url VARCHAR(500),
  music_track VARCHAR(255),
  is_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE,
  deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE post_media_items (
  id BIGSERIAL PRIMARY KEY,
  post_id VARCHAR(50) NOT NULL,
  url VARCHAR(500) NOT NULL,
  type VARCHAR(20) NOT NULL,
  position INTEGER NOT NULL,
  thumbnail_url VARCHAR(500),
  duration INTEGER,
  width INTEGER,
  height INTEGER,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_post_media_items_post_id ON post_media_items(post_id);
CREATE INDEX idx_post_media_items_position ON post_media_items(post_id, position);

CREATE TABLE post_tags (
  id BIGSERIAL PRIMARY KEY,
  post_id VARCHAR(50) NOT NULL,
  tag VARCHAR(100) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_post_tags_post_id ON post_tags(post_id);
CREATE INDEX idx_post_tags_tag ON post_tags(tag);

CREATE TABLE likes (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  post_id VARCHAR(50) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, post_id)
);

CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_user_id ON likes(user_id);

CREATE INDEX idx_user_id ON posts(user_id);
CREATE INDEX idx_mood ON posts(mood);
CREATE INDEX idx_created_at ON posts(created_at);
CREATE INDEX idx_post_type ON posts(post_type);
CREATE INDEX idx_deleted_at ON posts(deleted_at);
