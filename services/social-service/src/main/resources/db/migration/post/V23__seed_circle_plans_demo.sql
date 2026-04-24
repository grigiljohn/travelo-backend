-- Demo rows aligned with former in-memory PlanService seed (UUID ids for API consistency).

INSERT INTO circle_plans (
    id, host_user_id, organizer_community_id, title, description, location_label, time_label,
    max_people, joined_count, badge, hero_image_url, host_name, host_avatar_url,
    privacy, require_approval, allow_waitlist, status, created_at, updated_at
) VALUES
(
    'a1000000-0000-4000-8000-000000000001'::uuid,
    'system', NULL,
    'Sunset hike at 5 PM',
    'Easy trail, we meet at the main entrance. Bring water!',
    'Batu Caves',
    'Today · 5:00 PM',
    5, 3, 'TRENDING',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80',
    'Maya', 'https://picsum.photos/seed/circle-maya/200/200',
    'PUBLIC', false, true, 'PUBLISHED', NOW(), NOW()
),
(
    'a1000000-0000-4000-8000-000000000002'::uuid,
    'system', NULL,
    'Street food crawl — Jalan Alor',
    'Split small plates, ~RM40 each. Vegetarians welcome.',
    'Bukit Bintang',
    'Tonight · 7:30 PM',
    6, 2, 'HAPPENING_NOW',
    'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=1200&q=80',
    'Arjun', 'https://picsum.photos/seed/circle-arjun/200/200',
    'PUBLIC', false, true, 'PUBLISHED', NOW(), NOW()
),
(
    'a1000000-0000-4000-8000-000000000003'::uuid,
    'system', NULL,
    'KLCC park run & coffee',
    '5K easy pace, then specialty coffee nearby.',
    'KLCC',
    'Sat · 7:00 AM',
    8, 4, 'NONE',
    'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=1200&q=80',
    'Sofia', 'https://picsum.photos/seed/circle-sofia/200/200',
    'PUBLIC', false, true, 'PUBLISHED', NOW(), NOW()
);

INSERT INTO circle_plan_participants (plan_id, user_id, display_name, avatar_url, role) VALUES
('a1000000-0000-4000-8000-000000000001'::uuid, 'system', 'Maya', 'https://picsum.photos/seed/circle-maya/200/200', 'HOST'),
('a1000000-0000-4000-8000-000000000001'::uuid, 'seed-p1a', NULL, 'https://picsum.photos/seed/circle-p1a/200/200', 'MEMBER'),
('a1000000-0000-4000-8000-000000000001'::uuid, 'seed-p1b', NULL, 'https://picsum.photos/seed/circle-p1b/200/200', 'MEMBER'),

('a1000000-0000-4000-8000-000000000002'::uuid, 'system', 'Arjun', 'https://picsum.photos/seed/circle-arjun/200/200', 'HOST'),
('a1000000-0000-4000-8000-000000000002'::uuid, 'seed-p2a', NULL, 'https://picsum.photos/seed/circle-p2a/200/200', 'MEMBER'),

('a1000000-0000-4000-8000-000000000003'::uuid, 'system', 'Sofia', 'https://picsum.photos/seed/circle-sofia/200/200', 'HOST'),
('a1000000-0000-4000-8000-000000000003'::uuid, 'seed-p3a', NULL, 'https://picsum.photos/seed/circle-p3a/200/200', 'MEMBER'),
('a1000000-0000-4000-8000-000000000003'::uuid, 'seed-p3b', NULL, 'https://picsum.photos/seed/circle-p3b/200/200', 'MEMBER'),
('a1000000-0000-4000-8000-000000000003'::uuid, 'seed-p3c', NULL, 'https://picsum.photos/seed/circle-p3c/200/200', 'MEMBER');
