-- ============================================
-- Explore Feed Test Data Script
-- Database: travelo_posts
-- ============================================
-- 
-- This script creates test reels for the explore feed functionality.
-- It creates reels with various locations, captions, and metadata.
--
-- Usage:
--   psql -U travelo -d travelo_posts -f scripts/insert_explore_test_data.sql
--
-- ============================================

-- Enable UUID extension if not already enabled
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Insert test reels with various locations and content
-- Note: Replace USER_001, USER_002, etc. with actual user IDs from travelo_auth.users table

INSERT INTO posts (
    id,
    user_id,
    post_type,
    caption,
    mood,
    location,
    thumbnail_url,
    likes,
    comments,
    remixes,
    tips,
    shares,
    is_archived,
    is_verified,
    created_at,
    updated_at
) VALUES
-- Reels for Following/Explore/Nearby feeds
-- User 1 reels
(gen_random_uuid(), 'USER_001', 'reel', 'Amazing sunset at the beach! 🌅', 'chill', 'Miami Beach, FL', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/sunset.jpg', 1200, 45, 0, 0, 12, false, false, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(gen_random_uuid(), 'USER_001', 'reel', 'Exploring the city streets 🏙️', 'adventure', 'New York, NY', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/city.jpg', 850, 32, 0, 0, 8, false, false, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

-- User 2 reels
(gen_random_uuid(), 'USER_002', 'reel', 'Mountain hiking adventure ⛰️', 'adventure', 'Denver, CO', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/mountain.jpg', 2100, 78, 0, 0, 25, false, false, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(gen_random_uuid(), 'USER_002', 'reel', 'Food tour in the city 🍕', 'food', 'Chicago, IL', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/food.jpg', 950, 41, 0, 0, 15, false, false, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours'),

-- User 3 reels
(gen_random_uuid(), 'USER_003', 'reel', 'Beach vibes and good times 🏖️', 'chill', 'Los Angeles, CA', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/beach.jpg', 1800, 65, 0, 0, 20, false, false, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(gen_random_uuid(), 'USER_003', 'reel', 'Nightlife in the city 🌃', 'party', 'Las Vegas, NV', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/nightlife.jpg', 3200, 120, 0, 0, 45, false, false, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),

-- User 4 reels
(gen_random_uuid(), 'USER_004', 'reel', 'Nature walk in the forest 🌲', 'nature', 'Portland, OR', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/forest.jpg', 1400, 52, 0, 0, 18, false, false, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(gen_random_uuid(), 'USER_004', 'reel', 'Coffee shop vibes ☕', 'chill', 'Seattle, WA', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/coffee.jpg', 1100, 38, 0, 0, 10, false, false, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

-- User 5 reels
(gen_random_uuid(), 'USER_005', 'reel', 'Cultural experience in the city 🎭', 'culture', 'San Francisco, CA', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/culture.jpg', 1650, 58, 0, 0, 22, false, false, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(gen_random_uuid(), 'USER_005', 'reel', 'Romantic dinner date 💕', 'romantic', 'Paris, France', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/dinner.jpg', 2800, 95, 0, 0, 35, false, false, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

-- User 6 reels
(gen_random_uuid(), 'USER_006', 'reel', 'Adventure sports time! 🏄', 'adventure', 'Miami, FL', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/sports.jpg', 1950, 72, 0, 0, 28, false, false, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(gen_random_uuid(), 'USER_006', 'reel', 'Relaxing by the pool 🏊', 'relax', 'Phoenix, AZ', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/pool.jpg', 1300, 48, 0, 0, 16, false, false, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

-- User 7 reels
(gen_random_uuid(), 'USER_007', 'reel', 'Exploring local markets 🛍️', 'culture', 'Tokyo, Japan', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/market.jpg', 2400, 88, 0, 0, 30, false, false, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
(gen_random_uuid(), 'USER_007', 'reel', 'Mountain view from the top 🏔️', 'nature', 'Aspen, CO', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/mountain_view.jpg', 3100, 115, 0, 0, 42, false, false, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

-- User 8 reels
(gen_random_uuid(), 'USER_008', 'reel', 'Street art and graffiti 🎨', 'culture', 'Brooklyn, NY', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/street_art.jpg', 1750, 62, 0, 0, 24, false, false, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(gen_random_uuid(), 'USER_008', 'reel', 'Food festival fun! 🍔', 'food', 'Austin, TX', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/festival.jpg', 2200, 81, 0, 0, 33, false, false, NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours'),

-- User 9 reels
(gen_random_uuid(), 'USER_009', 'reel', 'Beach volleyball game 🏐', 'activity', 'San Diego, CA', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/volleyball.jpg', 1500, 55, 0, 0, 19, false, false, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(gen_random_uuid(), 'USER_009', 'reel', 'Sunset yoga session 🧘', 'relax', 'Santa Monica, CA', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/yoga.jpg', 1900, 68, 0, 0, 26, false, false, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

-- User 10 reels
(gen_random_uuid(), 'USER_010', 'reel', 'City lights at night ✨', 'chill', 'Boston, MA', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/city_lights.jpg', 2600, 92, 0, 0, 38, false, false, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(gen_random_uuid(), 'USER_010', 'reel', 'Hiking trail adventure 🥾', 'adventure', 'Boulder, CO', 'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/trail.jpg', 2100, 76, 0, 0, 29, false, false, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days')

ON CONFLICT (id) DO NOTHING;

-- Insert media items for reels (video URLs)
-- Note: This assumes you have post IDs from the above inserts
-- You may need to adjust based on your actual post IDs

-- Example: Insert media for first few reels
-- Replace post_id with actual IDs from the posts table
INSERT INTO post_media_items (
    post_id,
    url,
    type,
    position,
    thumbnail_url,
    duration
)
SELECT 
    p.id,
    'https://travelo-posts-main.s3.ap-south-1.amazonaws.com/uploads/videoplayback.mp4',
    'video',
    1,
    p.thumbnail_url,
    30 -- duration in seconds
FROM posts p
WHERE p.post_type = 'reel'
  AND p.id NOT IN (SELECT post_id FROM post_media_items)
LIMIT 20; -- Limit to avoid inserting duplicates

-- Verify inserted reels
SELECT 
    id,
    user_id,
    caption,
    location,
    likes,
    created_at
FROM posts 
WHERE post_type = 'reel'
ORDER BY created_at DESC
LIMIT 20;

-- ============================================
-- Notes:
-- ============================================
-- 1. Replace USER_001, USER_002, etc. with actual user IDs from travelo_auth.users
-- 2. Update S3 URLs with your actual media URLs
-- 3. Adjust locations based on your test data needs
-- 4. The script creates 20 reels across 10 users
-- 5. Media items are inserted for all reels automatically
--
-- ============================================

