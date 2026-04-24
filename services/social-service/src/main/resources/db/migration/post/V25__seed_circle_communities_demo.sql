-- Demo communities aligned with former CircleCommunityService @PostConstruct seed.

INSERT INTO circle_communities (id, name, description, city, visibility, owner_user_id, member_count, last_activity_label) VALUES
('g1', 'Munnar Weekend Travelers',
 'Weekend escapes, tea estates, and sunrise viewpoints.',
 'Kuala Lumpur', 'public', 'system', 2, '2m ago'),
('g2', 'Backpackers in KL',
 'Cheap eats, hostels, and night-market crawls.',
 'Kuala Lumpur', 'public', 'system', 1, '15m ago'),
('g3', 'Private Photo Walk',
 'Invite-only · street photography sessions.',
 'Kuala Lumpur', 'private', 'diana', 1, '1h ago'),
('g4', 'KL Night Photography Walk',
 'Rooftops & alley light trails after sunset.',
 'Kuala Lumpur', 'public', 'system', 1, '30m ago'),
('g5', 'Fusion Food Explorers',
 'Weekend hawker hops + modern tasting menus.',
 'Kuala Lumpur', 'public', 'system', 1, '1d ago'),
('g6', 'Kochi Backwaters & Spice',
 'Houseboats, fort walks, and home-style sadya on Sundays.',
 'Kochi, Kerala', 'public', 'system', 1, '3h ago'),
('g7', 'Fort Kochi Photo Walk',
 'Street frames, fishing nets at blue hour, small group only.',
 'Kochi, Kerala', 'public', 'system', 1, '6h ago'),
('g8', 'Bay Area Weekend Hikers',
 'Coastal trails, redwoods, and post-hike coffee.',
 'San Francisco, California', 'public', 'system', 1, '20m ago'),
('g9', 'Global Nomads Hub',
 'City-agnostic tips, remote-work meetups, and airport hacks.',
 '', 'public', 'system', 1, '4h ago');

INSERT INTO circle_community_tags (community_id, tag) VALUES
('g1', 'Hiking'), ('g1', 'Photography'), ('g1', 'Weekends'),
('g2', 'Food'), ('g2', 'Budget'), ('g2', 'Social'),
('g3', 'Photo'), ('g3', 'City'),
('g4', 'Photo'), ('g4', 'Night'), ('g4', 'City'),
('g5', 'Food'), ('g5', 'Social'),
('g6', 'Culture'), ('g6', 'Food'), ('g6', 'Weekends'),
('g7', 'Photo'), ('g7', 'Walking'), ('g7', 'City'),
('g8', 'Hiking'), ('g8', 'Nature'), ('g8', 'Social'),
('g9', 'Remote work'), ('g9', 'Travel tips'), ('g9', 'Social');

INSERT INTO circle_community_members (community_id, user_id, role) VALUES
('g1', 'alice', 'MEMBER'), ('g1', 'bob', 'MEMBER'),
('g2', 'carol', 'MEMBER'),
('g3', 'diana', 'MEMBER'),
('g4', 'alice', 'MEMBER'),
('g5', 'bob', 'MEMBER'),
('g6', 'eve', 'MEMBER'),
('g7', 'frank', 'MEMBER'),
('g8', 'grace', 'MEMBER'),
('g9', 'henry', 'MEMBER');
