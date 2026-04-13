-- Fix ownership of music_tracks table in travelo_music database
-- This script transfers ownership to the travelo user
-- Run this as a superuser (postgres user)
-- 
-- Usage: psql -U postgres -f infra/fix-music-table-ownership.sql

\connect travelo_music

-- Check if table exists and show current owner
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE schemaname = 'public' AND tablename = 'music_tracks';

-- Transfer ownership of the table to travelo user
ALTER TABLE IF EXISTS music_tracks OWNER TO travelo;

-- Also transfer ownership of any sequences related to the table
DO $$
DECLARE
    seq_record RECORD;
BEGIN
    FOR seq_record IN 
        SELECT sequence_name 
        FROM information_schema.sequences 
        WHERE sequence_schema = 'public' 
        AND sequence_name LIKE 'music_tracks%'
    LOOP
        EXECUTE format('ALTER SEQUENCE %I OWNER TO travelo', seq_record.sequence_name);
    END LOOP;
END $$;

-- Grant all privileges on the table to travelo (in case it's not the owner yet)
GRANT ALL PRIVILEGES ON TABLE music_tracks TO travelo;

-- Verify ownership
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE schemaname = 'public' AND tablename = 'music_tracks';

\connect postgres
SELECT 'Ownership fixed for music_tracks table' AS status;

