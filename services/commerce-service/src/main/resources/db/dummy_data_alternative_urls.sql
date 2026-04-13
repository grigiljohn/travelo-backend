-- ============================================================
-- Alternative URLs for Dummy Data
-- If the URLs in dummy_data.sql don't work, use these alternatives
-- ============================================================

-- Update Assets with alternative URLs
UPDATE assets 
SET url = 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&h=630&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=300&h=300&fit=crop'
WHERE id = '10000000-0000-0000-0000-000000000001';

UPDATE assets 
SET url = 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&h=630&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=300&h=300&fit=crop'
WHERE id = '10000000-0000-0000-0000-000000000002';

UPDATE assets 
SET url = 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&h=630&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=300&h=300&fit=crop'
WHERE id = '10000000-0000-0000-0000-000000000003';

-- For videos, you may need to use your own video URLs or test with these
-- NOTE: These are sample URLs that may or may not work
UPDATE assets 
SET url = 'https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4',
    thumbnail_url = 'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=720&h=1280&fit=crop'
WHERE id = '10000000-0000-0000-0000-000000000004';

UPDATE assets 
SET url = 'https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4',
    thumbnail_url = 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=720&h=1280&fit=crop'
WHERE id = '10000000-0000-0000-0000-000000000005';

-- Update Ad Creative JSONB fields
UPDATE ads 
SET creative = '{"imageUrl": "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&h=630&fit=crop", "title": "Travel Adventure"}'::jsonb
WHERE id = '40000000-0000-0000-0000-000000000001';

UPDATE ads 
SET creative = '{"imageUrl": "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&h=630&fit=crop", "title": "Hotel Booking"}'::jsonb
WHERE id = '40000000-0000-0000-0000-000000000002';

UPDATE ads 
SET creative = '{"imageUrl": "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200&h=630&fit=crop", "title": "Summer Deals"}'::jsonb
WHERE id = '40000000-0000-0000-0000-000000000003';

UPDATE ads 
SET creative = '{"videoUrl": "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "thumbnailUrl": "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=720&h=1280&fit=crop"}'::jsonb
WHERE id = '40000000-0000-0000-0000-000000000004';

UPDATE ads 
SET creative = '{"videoUrl": "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "thumbnailUrl": "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=720&h=1280&fit=crop"}'::jsonb
WHERE id = '40000000-0000-0000-0000-000000000005';

