-- ============================================================
-- Dummy Data for Ad-Service Database
-- Execute this script manually to insert test data
-- ============================================================
-- Usage: psql -h localhost -U travelo -d travelo_ads -f dummy_data.sql
-- ============================================================

-- ============================================================
-- 1. Business Accounts (required for campaigns)
-- ============================================================
INSERT INTO business_accounts (id, user_id, name, is_default, created_at, updated_at)
VALUES 
    ('00000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Travel Adventures Co.', true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'Hotel Booking Pro', true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333', 'Foodie Travel', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. Assets (Creative Images/Videos for Ads)
-- ============================================================
INSERT INTO assets (id, url, thumbnail_url, type, width, height, size, format, storage_provider, business_account_id, uploaded_by, created_at, updated_at)
VALUES 
    -- Feed Ad Assets (Images) - Using Picsum for reliable placeholder images
    ('10000000-0000-0000-0000-000000000001', 'https://picsum.photos/1200/630?random=1', 'https://picsum.photos/300/300?random=1', 'IMAGE', 1200, 630, 245760, 'jpg', 'S3', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000002', 'https://picsum.photos/1200/630?random=2', 'https://picsum.photos/300/300?random=2', 'IMAGE', 1200, 630, 245760, 'jpg', 'S3', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000003', 'https://picsum.photos/1200/630?random=3', 'https://picsum.photos/300/300?random=3', 'IMAGE', 1200, 630, 245760, 'jpg', 'S3', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', NOW(), NOW()),
    
    -- Reel Ad Assets (Videos) - Using test video URLs (replace with your own videos if these don't work)
    -- Alternative: Use your S3 bucket URLs or local test videos
    ('10000000-0000-0000-0000-000000000004', 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4', 'https://picsum.photos/720/1280?random=4', 'VIDEO', 720, 1280, 5242880, 'mp4', 'S3', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000005', 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_2MB.mp4', 'https://picsum.photos/720/1280?random=5', 'VIDEO', 720, 1280, 10485760, 'mp4', 'S3', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. Campaigns (Active campaigns for feed and reel ads)
-- ============================================================
INSERT INTO campaigns (id, business_account_id, name, objective, status, budget, budget_type, start_date, end_date, bid_strategy, bid_amount, pacing, optimization_goal, frequency_cap, targeting, placements, spend, impressions, clicks, ctr, conversions, created_by, created_at, updated_at)
VALUES 
    -- Feed Campaign 1
    ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Travel Adventures Feed Campaign', 'DISPLAY_ADS', 'ACTIVE', 5000.00, 'DAILY', NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days', 'CPC', 0.50, 'standard', 'link_click', '2 impressions / 7 days', '{}'::jsonb, '{"feed": true}'::jsonb, 0.00, 0, 0, 0.0, 0, '00000000-0000-0000-0000-000000000001', NOW(), NOW()),
    
    -- Feed Campaign 2
    ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'Hotel Booking Feed Campaign', 'DISPLAY_ADS', 'ACTIVE', 3000.00, 'DAILY', NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days', 'CPC', 0.75, 'standard', 'link_click', '3 impressions / 7 days', '{}'::jsonb, '{"feed": true}'::jsonb, 0.00, 0, 0, 0.0, 0, '00000000-0000-0000-0000-000000000002', NOW(), NOW()),
    
    -- Reel Campaign 1
    ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Travel Adventures Reel Campaign', 'VIDEO_ADS', 'ACTIVE', 7000.00, 'DAILY', NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days', 'CPM', 10.00, 'standard', 'video_view', '2 impressions / 7 days', '{}'::jsonb, '{"reel": true}'::jsonb, 0.00, 0, 0, 0.0, 0, '00000000-0000-0000-0000-000000000001', NOW(), NOW()),
    
    -- Reel Campaign 2
    ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003', 'Foodie Travel Reel Campaign', 'VIDEO_ADS', 'ACTIVE', 4000.00, 'DAILY', NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days', 'CPM', 8.00, 'standard', 'video_view', '2 impressions / 7 days', '{}'::jsonb, '{"reel": true}'::jsonb, 0.00, 0, 0, 0.0, 0, '00000000-0000-0000-0000-000000000003', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. Ad Groups (with placements for feed/reel)
-- ============================================================
INSERT INTO ad_groups (id, campaign_id, name, status, budget, budget_type, targeting, keywords, negative_keywords, devices, placements, created_at, updated_at)
VALUES 
    -- Feed Ad Groups
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Travel Adventures Feed Group 1', 'ACTIVE', 1000.00, 'DAILY', '{}'::jsonb, ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::varchar[], ARRAY['feed']::varchar[], NOW(), NOW()),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Hotel Booking Feed Group 1', 'ACTIVE', 800.00, 'DAILY', '{}'::jsonb, ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::varchar[], ARRAY['feed']::varchar[], NOW(), NOW()),
    
    -- Reel Ad Groups
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'Travel Adventures Reel Group 1', 'ACTIVE', 1500.00, 'DAILY', '{}'::jsonb, ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::varchar[], ARRAY['reel']::varchar[], NOW(), NOW()),
    ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', 'Foodie Travel Reel Group 1', 'ACTIVE', 1200.00, 'DAILY', '{}'::jsonb, ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::varchar[], ARRAY['reel']::varchar[], NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 5. Ads (with creatives and content)
-- ============================================================
INSERT INTO ads (id, ad_group_id, creative_id, name, status, ad_type, headline, headlines, description, descriptions, call_to_action, cta_text, brand_name, brand_website, creative, final_url, display_url, quality_score, ad_strength, impressions, clicks, ctr, conversions, created_at, updated_at)
VALUES 
    -- Feed Ads (IMAGE type)
    ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Travel Adventure Feed Ad 1', 'ACTIVE', 'IMAGE', 'Discover Amazing Destinations', ARRAY['Discover Amazing Destinations', 'Plan Your Next Adventure', 'Travel the World']::text[], 'Explore beautiful places and create unforgettable memories.', ARRAY['Explore beautiful places and create unforgettable memories.', 'Book your dream vacation today!']::text[], 'LEARN_MORE', 'Explore Now', 'Travel Adventures Co.', 'https://traveladventures.com', '{"imageUrl": "https://picsum.photos/1200/630?random=1", "title": "Travel Adventure"}'::jsonb, 'https://traveladventures.com/tours', 'traveladventures.com', 85, 90, 0, 0, 0.0, 0, NOW(), NOW()),
    
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Hotel Booking Feed Ad 1', 'ACTIVE', 'IMAGE', 'Best Hotels at Great Prices', ARRAY['Best Hotels at Great Prices', 'Book Now and Save', 'Your Perfect Stay Awaits']::text[], 'Find the perfect hotel for your trip.', ARRAY['Find the perfect hotel for your trip.', 'Special discounts available!']::text[], 'SHOP_NOW', 'Book Hotel', 'Hotel Booking Pro', 'https://hotelbooking.com', '{"imageUrl": "https://picsum.photos/1200/630?random=2", "title": "Hotel Booking"}'::jsonb, 'https://hotelbooking.com/search', 'hotelbooking.com', 80, 85, 0, 0, 0.0, 0, NOW(), NOW()),
    
    -- Additional Feed Ad for variety
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Travel Adventure Feed Ad 2', 'ACTIVE', 'RESPONSIVE', 'Summer Travel Deals', ARRAY['Summer Travel Deals', 'Limited Time Offer', 'Start Planning Today']::text[], 'Get up to 30% off on summer packages.', ARRAY['Get up to 30% off on summer packages.', 'Don''t miss out!']::text[], 'GET_OFFER', 'View Deals', 'Travel Adventures Co.', 'https://traveladventures.com/deals', '{"imageUrl": "https://picsum.photos/1200/630?random=10", "title": "Summer Deals"}'::jsonb, 'https://traveladventures.com/summer-deals', 'traveladventures.com/deals', 82, 88, 0, 0, 0.0, 0, NOW(), NOW()),
    
    -- Reel Ads (VIDEO type)
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', 'Travel Adventure Reel Ad 1', 'ACTIVE', 'VIDEO', 'Experience the World', ARRAY['Experience the World', 'Adventure Awaits', 'Travel Now']::text[], 'Watch amazing travel destinations and start planning.', ARRAY['Watch amazing travel destinations and start planning.', 'Swipe up to explore!']::text[], 'WATCH_MORE', 'Swipe Up', 'Travel Adventures Co.', 'https://traveladventures.com/reels', '{"videoUrl": "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4", "thumbnailUrl": "https://picsum.photos/720/1280?random=4"}'::jsonb, 'https://traveladventures.com', 'traveladventures.com', 90, 95, 0, 0, 0.0, 0, NOW(), NOW()),
    
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000005', 'Foodie Travel Reel Ad 1', 'ACTIVE', 'VIDEO', 'Taste the World', ARRAY['Taste the World', 'Food Adventures', 'Try New Flavors']::text[], 'Discover amazing food from around the globe.', ARRAY['Discover amazing food from around the globe.', 'Swipe up to learn more!']::text[], 'WATCH_MORE', 'Swipe Up', 'Foodie Travel', 'https://foodietravel.com/reels', '{"videoUrl": "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_2MB.mp4", "thumbnailUrl": "https://picsum.photos/720/1280?random=5"}'::jsonb, 'https://foodietravel.com', 'foodietravel.com', 88, 92, 0, 0, 0.0, 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Verify inserted data
-- ============================================================
SELECT 'Business Accounts' as table_name, COUNT(*) as count FROM business_accounts
UNION ALL
SELECT 'Assets', COUNT(*) FROM assets
UNION ALL
SELECT 'Campaigns (Active)', COUNT(*) FROM campaigns WHERE status = 'ACTIVE'
UNION ALL
SELECT 'Ad Groups (Active)', COUNT(*) FROM ad_groups WHERE status = 'ACTIVE'
UNION ALL
SELECT 'Ads (Active)', COUNT(*) FROM ads WHERE status = 'ACTIVE';

