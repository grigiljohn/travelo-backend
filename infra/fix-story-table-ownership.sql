-- Fix ownership of story tables in travelo_stories database
-- This script transfers ownership to the travelo user
-- Run this as a superuser (postgres user)
-- 
-- Usage: psql -U postgres -f infra/fix-story-table-ownership.sql

\connect travelo_stories

-- Check if tables exist and show current owner
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename IN ('stories', 'story_views', 'story_replies', 'story_highlights');

-- Transfer ownership of all story tables to travelo user
ALTER TABLE IF EXISTS stories OWNER TO travelo;
ALTER TABLE IF EXISTS story_views OWNER TO travelo;
ALTER TABLE IF EXISTS story_replies OWNER TO travelo;
ALTER TABLE IF EXISTS story_highlights OWNER TO travelo;

-- Also transfer ownership of any sequences related to the tables
DO $$
DECLARE
    seq_record RECORD;
BEGIN
    FOR seq_record IN 
        SELECT sequence_name 
        FROM information_schema.sequences 
        WHERE sequence_schema = 'public' 
        AND (sequence_name LIKE 'stories%' 
             OR sequence_name LIKE 'story_views%' 
             OR sequence_name LIKE 'story_replies%' 
             OR sequence_name LIKE 'story_highlights%')
    LOOP
        EXECUTE format('ALTER SEQUENCE %I OWNER TO travelo', seq_record.sequence_name);
    END LOOP;
END $$;

-- Transfer ownership of function
ALTER FUNCTION IF EXISTS delete_expired_stories() OWNER TO travelo;

-- Grant all privileges on all tables to travelo
GRANT ALL PRIVILEGES ON TABLE stories TO travelo;
GRANT ALL PRIVILEGES ON TABLE story_views TO travelo;
GRANT ALL PRIVILEGES ON TABLE story_replies TO travelo;
GRANT ALL PRIVILEGES ON TABLE story_highlights TO travelo;

-- Verify ownership
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename IN ('stories', 'story_views', 'story_replies', 'story_highlights');

\connect postgres
SELECT 'Ownership fixed for story tables' AS status;

