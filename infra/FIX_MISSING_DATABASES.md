# Fix Missing Databases - Quick Guide

## Problem
Multiple services are failing because these databases don't exist:
- `travelo_stories`
- `travelo_reels`
- `travelo_notifications`
- `travelo_messaging`
- `travelo_users`
- `travelo_admin`

## Quick Fix (Choose One Method)

### Method 1: Run PowerShell Script ⭐ (Easiest)
```powershell
cd infra
.\create-missing-databases.ps1
```

### Method 2: Run SQL Script Directly
```powershell
cd infra
Get-Content create-missing-databases.sql | docker exec -i infra-postgres-1 psql -U travelo -d postgres
```

### Method 3: One-Line Command
```powershell
docker exec infra-postgres-1 psql -U travelo -d postgres -c "CREATE DATABASE travelo_stories;" -c "CREATE DATABASE travelo_reels;" -c "CREATE DATABASE travelo_notifications;" -c "CREATE DATABASE travelo_messaging;" -c "CREATE DATABASE travelo_users;" -c "CREATE DATABASE travelo_admin;"
```

### Method 4: Interactive PostgreSQL
```powershell
docker exec -it infra-postgres-1 psql -U travelo -d postgres
```

Then run:
```sql
CREATE DATABASE travelo_stories;
CREATE DATABASE travelo_reels;
CREATE DATABASE travelo_notifications;
CREATE DATABASE travelo_messaging;
CREATE DATABASE travelo_users;
CREATE DATABASE travelo_admin;
```

## Verify Databases Created

```powershell
docker exec infra-postgres-1 psql -U travelo -d postgres -c "\l" | Select-String "travelo"
```

## Restart Services

After creating databases, restart the services:

```powershell
docker compose restart story-service notification-service reel-service messaging-service admin-service
```

---

**Note**: If PostgreSQL container is not running, start it first:
```powershell
docker compose up postgres
```

