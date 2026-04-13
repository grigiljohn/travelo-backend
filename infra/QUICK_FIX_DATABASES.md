# Quick Fix: Create Missing Databases

## Problem
Multiple services are failing because databases don't exist:
- `travelo_stories` (story-service)
- `travelo_notifications` (notification-service)
- `travelo_reels`, `travelo_messaging`, `travelo_users`, `travelo_admin`

PostgreSQL initialization scripts only run on first startup when the data directory is empty.

## Quick Fix - Run This Command

### PowerShell (Windows):
```powershell
cd infra
Get-Content create-missing-databases.sql | docker exec -i infra-postgres-1 psql -U travelo -d postgres
```

### Or use the script:
```powershell
cd infra
.\create-missing-databases.ps1
```

### Bash/Linux/Mac:
```bash
cd infra
docker exec -i infra-postgres-1 psql -U travelo -d postgres < create-missing-databases.sql
```

## Alternative: One-Line Fix

If PostgreSQL container is already running:

```powershell
docker exec -i infra-postgres-1 psql -U travelo -d postgres -c "CREATE DATABASE travelo_stories;" -c "CREATE DATABASE travelo_reels;" -c "CREATE DATABASE travelo_notifications;" -c "CREATE DATABASE travelo_messaging;" -c "CREATE DATABASE travelo_users;" -c "CREATE DATABASE travelo_admin;"
```

## Verify Databases Created

```powershell
docker exec -it infra-postgres-1 psql -U travelo -d postgres -c "\l" | Select-String "travelo"
```

You should see all 9 databases:
- travelo_posts
- travelo_ads
- travelo_media
- travelo_notifications
- travelo_messaging
- travelo_stories
- travelo_reels
- travelo_users
- travelo_admin

## After Creating Databases

Restart the failing services:

```powershell
docker compose restart story-service notification-service
```

Or restart all services:

```powershell
docker compose restart
```

