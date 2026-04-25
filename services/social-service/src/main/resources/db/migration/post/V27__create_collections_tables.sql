CREATE TABLE IF NOT EXISTS collections (
  id UUID PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  title VARCHAR(140) NOT NULL,
  type VARCHAR(20) NOT NULL,
  trip_id VARCHAR(80),
  cover_image_url VARCHAR(600),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS collection_media (
  id UUID PRIMARY KEY,
  collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
  media_url VARCHAR(800) NOT NULL,
  thumbnail_url VARCHAR(800),
  media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
  source_type VARCHAR(20) NOT NULL DEFAULT 'DEVICE',
  source_id VARCHAR(120),
  captured_at TIMESTAMP WITH TIME ZONE,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_collections_user_type ON collections(user_id, type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_collections_trip_id ON collections(trip_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_collections_user_auto_trip ON collections(user_id, type, trip_id);
CREATE INDEX IF NOT EXISTS idx_collection_media_collection_sort ON collection_media(collection_id, sort_order, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_collection_media_captured_at ON collection_media(collection_id, captured_at DESC);
CREATE INDEX IF NOT EXISTS idx_collection_media_geo ON collection_media(collection_id, latitude, longitude);
