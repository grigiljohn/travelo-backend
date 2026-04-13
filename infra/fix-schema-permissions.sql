-- Fix PostgreSQL schema permissions for all Travelo databases
-- This script grants necessary permissions on the public schema
-- Run this as a superuser (postgres user)

\connect postgres

-- Function to grant permissions on a database
DO $$
DECLARE
    db_name TEXT;
    db_list TEXT[] := ARRAY[
        'travelo_posts',
        'travelo_ads',
        'travelo_media',
        'travelo_notifications',
        'travelo_messaging',
        'travelo_stories',
        'travelo_reels',
        'travelo_users',
        'travelo_admin',
        'travelo_auth',
        'travelo_music'
    ];
BEGIN
    FOREACH db_name IN ARRAY db_list
    LOOP
        -- Check if database exists
        IF EXISTS (SELECT 1 FROM pg_database WHERE datname = db_name) THEN
            -- Grant permissions on public schema
            EXECUTE format('GRANT USAGE ON SCHEMA public TO travelo');
            EXECUTE format('GRANT CREATE ON SCHEMA public TO travelo');
            EXECUTE format('GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo');
            EXECUTE format('GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo');
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo');
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo');
            
            RAISE NOTICE 'Granted permissions on database: %', db_name;
        ELSE
            RAISE NOTICE 'Database does not exist: %', db_name;
        END IF;
    END LOOP;
END $$;

-- Connect to each database and grant permissions directly
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
SELECT 'Permissions fixed for all Travelo databases' AS status;

