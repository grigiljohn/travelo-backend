-- Curated one-tap trip templates for the mobile travel planner (TripPreferences shape in JSONB).

CREATE TABLE predefined_trips (
    id                 BIGSERIAL PRIMARY KEY,
    slug               VARCHAR(120)  NOT NULL UNIQUE,
    title              VARCHAR(200)  NOT NULL,
    subtitle           VARCHAR(500)  NOT NULL DEFAULT '',
    hero_image_url     TEXT          NOT NULL DEFAULT '',
    sort_order         INT           NOT NULL DEFAULT 0,
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    estimated_days     INT,
    trip_preferences   JSONB         NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_predefined_trips_active_sort ON predefined_trips (is_active, sort_order);

INSERT INTO predefined_trips (slug, title, subtitle, hero_image_url, sort_order, is_active, estimated_days, trip_preferences)
VALUES
(
    'malaysia-escape',
    'Malaysia escape',
    '5 days · culture & city',
    'https://images.unsplash.com/photo-1596422846543-75c6fc197f07?w=1200&q=80',
    0,
    TRUE,
    5,
    '{"destination": "Kuala Lumpur, Malaysia", "dateInfo": {"numberOfDays": 5}, "budget": "medium", "companions": ["couple"], "activities": ["city_sightseeing", "food_tours", "museums"]}'::jsonb
),
(
    'munnar-hills',
    'Munnar hills',
    'Nature · 2–3 days',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80',
    1,
    TRUE,
    3,
    '{"destination": "Munnar, India", "dateInfo": {"numberOfDays": 3}, "budget": "low", "companions": ["friends"], "activities": ["hiking", "nature", "photography"]}'::jsonb
),
(
    'bali-weekend',
    'Bali weekend',
    'Relax · beach & wellness',
    'https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=1200&q=80',
    2,
    TRUE,
    4,
    '{"destination": "Bali, Indonesia", "dateInfo": {"numberOfDays": 4}, "budget": "medium", "companions": ["couple"], "activities": ["beaches", "wellness", "temples"]}'::jsonb
),
(
    'tokyo-nights',
    'Tokyo nights',
    '3 days · urban energy',
    'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=1200&q=80',
    3,
    TRUE,
    3,
    '{"destination": "Tokyo, Japan", "dateInfo": {"numberOfDays": 3}, "budget": "high", "companions": ["solo"], "activities": ["nightlife", "food_tours", "city_sightseeing"]}'::jsonb
);
