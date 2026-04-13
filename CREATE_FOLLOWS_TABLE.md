# Create Follows Table - Quick Fix

## Problem
The `follows` table is missing from the database, causing 500 errors when loading user profiles.

## Solution

### Option 1: Run the SQL Script Directly

Connect to your PostgreSQL database and run:

```sql
-- Create follows table for user follow relationships
CREATE TABLE IF NOT EXISTS follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_followee FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_follow UNIQUE (follower_id, followee_id),
    CONSTRAINT check_not_self_follow CHECK (follower_id != followee_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_follower_id ON follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_followee_id ON follows(followee_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_follower_followee ON follows(follower_id, followee_id);
```

### Option 2: Use psql Command Line

```bash
# Connect to database
psql -U travelo -d travelo_auth

# Run the script
\i scripts/create_follows_table.sql
```

### Option 3: Use Docker (if using Docker)

```bash
# If database is in Docker
docker exec -i travelo-postgres psql -U travelo -d travelo_auth < scripts/create_follows_table.sql
```

## Verification

After creating the table, verify it exists:

```sql
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name = 'follows';
```

## Temporary Workaround

I've added error handling to the backend code that will:
- Catch the missing table error
- Return default values (0 followers, 0 following)
- Log a warning instead of crashing

The app will work, but follow counts will show as 0 until the table is created.

## Next Steps

1. **Create the table** using one of the options above
2. **Restart the user-service** to clear any cached errors
3. **Test the profile endpoint** - it should now work correctly

