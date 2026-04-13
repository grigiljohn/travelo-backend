-- V7: Create post_templates table for AI-enhanced post templates
CREATE TABLE IF NOT EXISTS post_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id VARCHAR(50) UNIQUE NOT NULL,
    label VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    thumbnail_url VARCHAR(500),
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index for active templates ordered by display order
CREATE INDEX IF NOT EXISTS idx_post_templates_active_order ON post_templates(is_active, display_order) WHERE is_active = TRUE;

-- Insert default templates (id + timestamps: legacy tables may omit column DEFAULTs)
INSERT INTO post_templates (id, template_id, label, icon, is_default, is_active, display_order, created_at, updated_at) VALUES
    (gen_random_uuid(), 'revert', 'Revert', 'close', FALSE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'edit', 'Embrace l...', 'edit', TRUE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'share', 'Share wo...', 'image', FALSE, TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'enjoy', 'Enjoy you...', 'image', FALSE, TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'continue', 'continue...', 'image', FALSE, TRUE, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'origin', 'Origin', 'image', FALSE, TRUE, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (template_id) DO NOTHING;

-- Optional: role "travelo" may not exist in local dev (e.g. only user "postgres")
DO $$
BEGIN
  ALTER TABLE post_templates OWNER TO travelo;
  GRANT ALL PRIVILEGES ON TABLE post_templates TO travelo;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

