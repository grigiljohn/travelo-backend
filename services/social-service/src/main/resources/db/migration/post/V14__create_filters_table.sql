-- V14: Create filters table for camera filters and beauty effects
CREATE TABLE IF NOT EXISTS filters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'filter' or 'beauty'
    preview_url VARCHAR(500),
    config JSONB, -- Filter configuration: {"brightness": 0.1, "contrast": 1.2, "saturation": 0.9}
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_filters_type ON filters(type, display_order) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_filters_active ON filters(is_active) WHERE is_active = TRUE;

DO $$
BEGIN
  ALTER TABLE filters OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE filters TO travelo;
  GRANT ALL PRIVILEGES ON TABLE filters TO PUBLIC;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

