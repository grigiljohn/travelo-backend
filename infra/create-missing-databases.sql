-- Script to create missing databases in existing PostgreSQL instance
-- Run this script manually if databases are missing
-- Also grants necessary permissions on the public schema

\connect postgres

-- Create databases if they don't exist
SELECT 'CREATE DATABASE travelo_posts'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_posts')\gexec

SELECT 'CREATE DATABASE travelo_ads'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_ads')\gexec

SELECT 'CREATE DATABASE travelo_media'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_media')\gexec

SELECT 'CREATE DATABASE travelo_notifications'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_notifications')\gexec

SELECT 'CREATE DATABASE travelo_messaging'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_messaging')\gexec

SELECT 'CREATE DATABASE travelo_stories'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_stories')\gexec

SELECT 'CREATE DATABASE travelo_reels'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_reels')\gexec

SELECT 'CREATE DATABASE travelo_users'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_users')\gexec

SELECT 'CREATE DATABASE travelo_admin'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_admin')\gexec

SELECT 'CREATE DATABASE travelo_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_auth')\gexec

SELECT 'CREATE DATABASE travelo_music'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'travelo_music')\gexec

-- Grant permissions on public schema for all databases
-- This fixes "permission denied for schema public" errors (especially in PostgreSQL 15+)

\connect travelo_posts
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_ads
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_media
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_notifications
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_messaging
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_stories
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_reels
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_users
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_admin
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_auth
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect travelo_music
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;

\connect postgres
-- List all databases to verify
\l

