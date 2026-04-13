-- V11: Create locations table for location tagging
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    address TEXT,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    place_id VARCHAR(255),
    country_code VARCHAR(2),
    city VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_locations_name ON locations(name);
CREATE INDEX IF NOT EXISTS idx_locations_place_id ON locations(place_id) WHERE place_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_locations_coordinates ON locations(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_locations_active ON locations(is_active) WHERE is_active = TRUE;

-- Create post_locations junction table
CREATE TABLE IF NOT EXISTS post_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, location_id)
);

CREATE INDEX IF NOT EXISTS idx_post_locations_post_id ON post_locations(post_id);
CREATE INDEX IF NOT EXISTS idx_post_locations_location_id ON post_locations(location_id);

DO $$
BEGIN
  ALTER TABLE locations OWNER TO travelo;
  ALTER TABLE post_locations OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE locations TO travelo;
  GRANT ALL PRIVILEGES ON TABLE post_locations TO travelo;
  GRANT ALL PRIVILEGES ON TABLE locations TO PUBLIC;
  GRANT ALL PRIVILEGES ON TABLE post_locations TO PUBLIC;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

