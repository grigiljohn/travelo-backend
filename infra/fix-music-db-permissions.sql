-- Fix PostgreSQL schema permissions for travelo_music database
-- This script grants necessary permissions on the public schema
-- Run this as a superuser (postgres user)
-- 
-- Usage: psql -U postgres -f fix-music-db-permissions.sql

\connect postgres

-- Create database if it doesn't exist
SELECT 'CREATE DATABASE travelo_music'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_music')\gexec

-- Connect to music database and grant permissions
\connect travelo_music

-- Grant permissions on public schema
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO travelo;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO travelo;

-- Also grant to public role (for Flyway schema history table)
GRANT USAGE ON SCHEMA public TO PUBLIC;
GRANT CREATE ON SCHEMA public TO PUBLIC;

\connect postgres
SELECT 'Permissions fixed for travelo_music database' AS status;

