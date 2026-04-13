# Explore Feature Setup Guide

This guide explains how to set up and use the new Explore feature with Following, Explore, and Nearby tabs.

## Overview

The Explore feature provides three main tabs:
1. **Following** - Shows reels from users you follow (or user suggestions if no follows)
2. **Explore** - Generic explore feed with randomized content
3. **Nearby** - Location-based feed (requires location permissions)

## Database Setup

### 1. Create Follows Table

```bash
psql -U travelo -d travelo_auth -f scripts/create_follows_table.sql
```

This creates the `follows` table to track user follow relationships.

### 2. Insert Test Users

```bash
psql -U travelo -d travelo_auth -f scripts/insert_test_users.sql
```

This creates 10 test users with password `password123`.

### 3. Insert Test Reels

```bash
psql -U travelo -d travelo_posts -f scripts/insert_explore_test_data.sql
```

**Important**: Before running this script:
1. Replace `USER_001`, `USER_002`, etc. with actual user IDs from your `travelo_auth.users` table
2. Update S3 URLs with your actual media URLs
3. Adjust locations as needed

### 4. Create Follow Relationships (Optional)

After inserting users, create some follow relationships:

```sql
-- Connect to travelo_auth database
psql -U travelo -d travelo_auth

-- Get user IDs
SELECT id, username FROM users LIMIT 10;

-- Insert follow relationships (replace UUIDs with actual user IDs)
INSERT INTO follows (follower_id, following_id) VALUES
    ('<user1_id>', '<user2_id>'),
    ('<user1_id>', '<user3_id>'),
    ('<user2_id>', '<user1_id>')
ON CONFLICT (follower_id, following_id) DO NOTHING;
```

## Backend Endpoints

### Following Feed
```
GET /api/v1/search/feed/following?page=1&limit=20
Headers: X-User-Id: <user_id>
```

### Explore Feed
```
GET /api/v1/search/feed/explore?page=1&limit=20
Headers: X-User-Id: <user_id> (optional)
```

### Nearby Feed
```
GET /api/v1/search/feed/nearby?lat=40.7128&lng=-74.0060&page=1&limit=20
Headers: X-User-Id: <user_id> (optional)
```

### User Suggestions
```
GET /api/v1/search/users/suggestions?page=1&limit=20
Headers: X-User-Id: <user_id> (optional)
```

## Flutter Implementation

### Main Components

1. **ExplorePage** (`lib/features/search/presentation/pages/explore_page.dart`)
   - Main page with 3 tabs
   - Hamburger menu, tabs, and search icon

2. **FollowingFeedWidget** (`lib/features/search/presentation/widgets/following_feed_widget.dart`)
   - Shows reels from followed users
   - Falls back to user suggestions if no follows

3. **ExploreFeedWidget** (`lib/features/search/presentation/widgets/explore_feed_widget.dart`)
   - Generic explore feed with randomized content

4. **NearbyFeedWidget** (`lib/features/search/presentation/widgets/nearby_feed_widget.dart`)
   - Location-based feed
   - Requests location permissions if not enabled

5. **TwoColumnReelWidget** (`lib/features/search/presentation/widgets/two_column_reel_widget.dart`)
   - Displays 2 reels side by side
   - Auto-plays random videos (30% of reels) without sound
   - No captions, likes, or comments shown

6. **UserSuggestionsWidget** (`lib/features/search/presentation/widgets/user_suggestions_widget.dart`)
   - Shows users to follow when user has no follows

### Features

- **2-Column Layout**: Reels displayed side by side (like Instagram)
- **Auto-Play Videos**: Randomly selected videos (30%) auto-play muted
- **No UI Overlays**: No captions, likes, or comments on grid items
- **Location Permissions**: Nearby tab requests location if not enabled
- **Pull-to-Refresh**: All feeds support pull-to-refresh
- **Infinite Scroll**: Automatic loading of more content

## Testing

### 1. Test Following Feed
- Login as a user
- Follow some users (or ensure user has no follows)
- Navigate to Explore tab → Following
- Should see reels from followed users OR user suggestions

### 2. Test Explore Feed
- Navigate to Explore tab → Explore
- Should see randomized reels
- Some videos should auto-play (muted)

### 3. Test Nearby Feed
- Navigate to Explore tab → Nearby
- If location not enabled, should see permission request
- After enabling, should see location-based reels

## Troubleshooting

### No reels showing
1. Check if reels are indexed in Elasticsearch:
   ```bash
   curl -X POST "http://localhost:8088/api/v1/admin/reindex/reels"
   ```

2. Verify posts exist in database:
   ```sql
   SELECT COUNT(*) FROM posts WHERE post_type = 'reel';
   ```

### Location not working
1. Check app permissions in device settings
2. Verify `geolocator` package is properly configured
3. Check location permissions in AndroidManifest.xml / Info.plist

### Videos not auto-playing
1. Check video URLs are valid and accessible
2. Verify `video_player` package is working
3. Check network connectivity

## Next Steps

1. **Enhance Following Logic**: Integrate with actual follow relationships from database
2. **Location-Based Filtering**: Implement proper geo-queries in Elasticsearch
3. **User Preferences**: Add personalization based on user interactions
4. **Follow Functionality**: Implement follow/unfollow buttons in user suggestions

## API Response Format

All feed endpoints return:
```json
{
  "success": true,
  "data": [
    {
      "type": "post",
      "id": "...",
      "title": "...",
      "subtitle": "...",
      "imageUrl": "...",
      "metadata": {
        "videoUrl": "...",
        "thumbnailUrl": "...",
        "username": "...",
        "likes": 0,
        ...
      }
    }
  ],
  "total": 20,
  "page": 1,
  "limit": 20,
  "hasMore": true
}
```

