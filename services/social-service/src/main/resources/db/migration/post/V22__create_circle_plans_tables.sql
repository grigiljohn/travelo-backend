-- Circle plans / events: persisted plans for GET/POST /api/v1/plans (social-service).
-- Rich plan feed (/api/v1/plans/feed) remains in-memory until unified on this schema (see RichPlanService).

CREATE TABLE circle_plans (
    id                     UUID PRIMARY KEY,
    host_user_id           VARCHAR(64)  NOT NULL,
    organizer_community_id VARCHAR(64),
    title                  VARCHAR(200) NOT NULL,
    description            VARCHAR(4000) NOT NULL DEFAULT '',
    location_label         VARCHAR(300) NOT NULL,
    external_place_id      VARCHAR(128),
    latitude               DOUBLE PRECISION,
    longitude              DOUBLE PRECISION,
    starts_at              TIMESTAMPTZ,
    time_label             VARCHAR(200) NOT NULL,
    max_people             INT          NOT NULL,
    joined_count           INT          NOT NULL DEFAULT 1,
    badge                  VARCHAR(24)  NOT NULL DEFAULT 'NONE',
    hero_image_url         VARCHAR(2000) NOT NULL DEFAULT '',
    host_name              VARCHAR(120) NOT NULL,
    host_avatar_url        VARCHAR(2000) NOT NULL DEFAULT '',
    privacy                VARCHAR(32)  NOT NULL DEFAULT 'PUBLIC',
    require_approval       BOOLEAN      NOT NULL DEFAULT FALSE,
    allow_waitlist         BOOLEAN      NOT NULL DEFAULT TRUE,
    status                 VARCHAR(24)  NOT NULL DEFAULT 'PUBLISHED',
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_circle_plans_max_people CHECK (max_people >= 2 AND max_people <= 500),
    CONSTRAINT chk_circle_plans_joined CHECK (joined_count >= 1)
);

CREATE INDEX idx_circle_plans_host_user ON circle_plans (host_user_id);
CREATE INDEX idx_circle_plans_created_at ON circle_plans (created_at DESC);
CREATE INDEX idx_circle_plans_starts_at ON circle_plans (starts_at DESC NULLS LAST);
CREATE INDEX idx_circle_plans_community ON circle_plans (organizer_community_id) WHERE organizer_community_id IS NOT NULL;

CREATE TABLE circle_plan_participants (
    id           BIGSERIAL PRIMARY KEY,
    plan_id      UUID         NOT NULL REFERENCES circle_plans (id) ON DELETE CASCADE,
    user_id      VARCHAR(64)  NOT NULL,
    display_name VARCHAR(120),
    avatar_url   VARCHAR(2000) NOT NULL DEFAULT '',
    role         VARCHAR(16)  NOT NULL DEFAULT 'MEMBER',
    joined_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_circle_plan_participant UNIQUE (plan_id, user_id)
);

CREATE INDEX idx_circle_plan_participants_plan ON circle_plan_participants (plan_id);
CREATE INDEX idx_circle_plan_participants_user ON circle_plan_participants (user_id);

COMMENT ON TABLE circle_plans IS 'Published circle events / travel plans (simple list + create from mobile).';
COMMENT ON COLUMN circle_plans.organizer_community_id IS 'Optional hosting community id when event is posted as a group.';
COMMENT ON COLUMN circle_plans.external_place_id IS 'Google/Manual place id for future geosearch.';
COMMENT ON TABLE circle_plan_participants IS 'RSVP rows; host row created on publish; joined_count denormalized on circle_plans.';
