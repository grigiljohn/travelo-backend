-- Fix permissions for travelo_messaging database
-- Run this script as the postgres superuser

-- Connect to the messaging database
\connect travelo_messaging

-- Grant all necessary permissions on the public schema
GRANT USAGE ON SCHEMA public TO travelo;
GRANT CREATE ON SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO travelo;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO travelo;

-- Set default privileges for future tables and sequences
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO travelo;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO travelo;

-- Grant ownership of the schema to travelo user (optional, but ensures full control)
-- ALTER SCHEMA public OWNER TO travelo;

SELECT 'Permissions granted successfully for travelo_messaging database' AS status;

