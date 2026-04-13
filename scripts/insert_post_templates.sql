-- Manual script to insert post templates into post-service database
-- Run this if templates are not showing up in the app
-- Usage: psql -U travelo -d travelo_posts -f scripts/insert_post_templates.sql

-- Ensure table exists (created by Hibernate ddl-auto: update)
-- If table doesn't exist, Hibernate will create it on next startup

-- Insert default templates (idempotent - won't duplicate)
INSERT INTO post_templates (template_id, label, icon, is_default, is_active, display_order) 
VALUES ('revert', 'Revert', 'close', FALSE, TRUE, 1)
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO post_templates (template_id, label, icon, is_default, is_active, display_order) 
VALUES ('edit', 'Embrace l...', 'edit', TRUE, TRUE, 2)
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO post_templates (template_id, label, icon, is_default, is_active, display_order) 
VALUES ('share', 'Share wo...', 'image', FALSE, TRUE, 3)
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO post_templates (template_id, label, icon, is_default, is_active, display_order) 
VALUES ('enjoy', 'Enjoy you...', 'image', FALSE, TRUE, 4)
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO post_templates (template_id, label, icon, is_default, is_active, display_order) 
VALUES ('continue', 'continue...', 'image', FALSE, TRUE, 5)
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO post_templates (template_id, label, icon, is_default, is_active, display_order) 
VALUES ('origin', 'Origin', 'image', FALSE, TRUE, 6)
ON CONFLICT (template_id) DO NOTHING;

-- Verify insertion
SELECT template_id, label, is_default, is_active, display_order 
FROM post_templates 
ORDER BY display_order;

