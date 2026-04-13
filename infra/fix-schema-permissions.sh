#!/bin/bash
# Bash script to fix PostgreSQL schema permissions
# This script grants necessary permissions on the public schema for all Travelo databases

set -e

HOST="${POSTGRES_HOST:-localhost}"
PORT="${POSTGRES_PORT:-5432}"
SUPER_USER="${POSTGRES_SUPERUSER:-postgres}"
APP_USER="${POSTGRES_USER:-travelo}"

echo "Fixing PostgreSQL schema permissions for Travelo databases..."

# Database list
DATABASES=(
    "travelo_posts"
    "travelo_ads"
    "travelo_media"
    "travelo_notifications"
    "travelo_messaging"
    "travelo_stories"
    "travelo_reels"
    "travelo_users"
    "travelo_admin"
    "travelo_auth"
)

for db in "${DATABASES[@]}"; do
    echo "Fixing permissions for database: $db"
    
    # Check if database exists
    if psql -h "$HOST" -p "$PORT" -U "$SUPER_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$db'" | grep -q 1; then
        # Grant permissions
        psql -h "$HOST" -p "$PORT" -U "$SUPER_USER" -d "$db" <<EOF
GRANT USAGE ON SCHEMA public TO $APP_USER;
GRANT CREATE ON SCHEMA public TO $APP_USER;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $APP_USER;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $APP_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $APP_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $APP_USER;
EOF
        
        if [ $? -eq 0 ]; then
            echo "  ✓ Permissions granted for $db"
        else
            echo "  ✗ Failed to grant permissions for $db"
        fi
    else
        echo "  ⚠ Database $db does not exist, skipping..."
    fi
done

echo ""
echo "Schema permissions fix completed!"

