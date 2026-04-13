-- Script to sync users from PostgreSQL to Elasticsearch
-- This script publishes user.updated events for all existing users
-- The search-service will consume these events and index users in Elasticsearch

-- Note: This requires Kafka to be running and the search-service to be listening
-- Run this script after ensuring Kafka and search-service are running

-- Option 1: If you have a Kafka producer function/trigger in PostgreSQL
-- (This would need to be set up separately)

-- Option 2: Manual approach - Query users and manually trigger events
-- You can use this query to get all users that need to be synced:

SELECT 
    id,
    username,
    name as display_name,
    email,
    is_email_verified as is_verified,
    created_at,
    updated_at
FROM travelo_auth.users
WHERE deleted_at IS NULL
ORDER BY created_at;

-- To actually sync users, you have a few options:

-- OPTION A: Use a Kafka producer tool/script
-- You would need to:
-- 1. Query the users from PostgreSQL
-- 2. For each user, publish a "user.updated" event to Kafka topic "user.updated"
-- 3. The event should contain:
--    {
--      "userId": "<user_id>",
--      "username": "<username>",
--      "displayName": "<name>",
--      "email": "<email>",
--      "isVerified": <is_email_verified>,
--      "isPrivate": false,
--      "followerCount": 0,
--      "followingCount": 0
--    }

-- OPTION B: Create a Java/Spring Boot script that:
-- 1. Connects to PostgreSQL auth database
-- 2. Fetches all users
-- 3. Publishes user.updated events to Kafka
-- 4. Search-service will automatically index them

-- OPTION C: Use the reindexing endpoint (if we add it)
-- POST http://localhost:8087/api/v1/admin/reindex/users

-- For now, the quickest solution is to:
-- 1. Ensure Kafka is running
-- 2. Ensure search-service is running and listening to "user.updated" topic
-- 3. Manually update a user in the auth-service (which will trigger user.updated event)
-- 4. Or create a simple script to publish events for all users

