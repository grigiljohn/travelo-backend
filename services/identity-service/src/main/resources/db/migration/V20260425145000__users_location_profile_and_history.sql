-- Persist user location consent + latest known location on profile,
-- plus append-only location history for analytics/timeline.

ALTER TABLE users ADD COLUMN IF NOT EXISTS location_permission_granted boolean NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS location_required boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_latitude double precision;
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_longitude double precision;
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_location_label varchar(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_city varchar(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_country varchar(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_location_updated_at timestamptz;

CREATE TABLE IF NOT EXISTS user_location_history (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    location_label varchar(255),
    city varchar(120),
    country varchar(120),
    source varchar(40),
    captured_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_location_history_user_id
    ON user_location_history(user_id);

CREATE INDEX IF NOT EXISTS idx_user_location_history_user_captured_at
    ON user_location_history(user_id, captured_at DESC);
