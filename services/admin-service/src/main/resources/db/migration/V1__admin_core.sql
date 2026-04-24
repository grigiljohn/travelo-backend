-- Admin service schema: auth, master data, moderation, flags, audit

CREATE TABLE admin_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(120) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'MODERATOR')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE admin_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    icon        VARCHAR(200) NOT NULL DEFAULT '',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_categories_name ON admin_categories (lower(name));
CREATE INDEX idx_admin_categories_active ON admin_categories (is_active);

CREATE TABLE admin_tags (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_admin_tags_slug UNIQUE (slug)
);
CREATE INDEX idx_admin_tags_name ON admin_tags (lower(name));

CREATE TABLE admin_communities (
    id            BIGSERIAL PRIMARY KEY,
    external_id   VARCHAR(64),
    name          VARCHAR(200)  NOT NULL,
    description   TEXT          NOT NULL DEFAULT '',
    city          VARCHAR(200)  NOT NULL DEFAULT '',
    visibility    VARCHAR(16)   NOT NULL DEFAULT 'public',
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_admin_communities_vis CHECK (visibility IN ('public', 'private', 'secret'))
);
CREATE INDEX idx_admin_communities_name ON admin_communities (lower(name));
CREATE INDEX idx_admin_communities_external ON admin_communities (external_id);

CREATE TABLE admin_locations (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    country       VARCHAR(100) NOT NULL DEFAULT '',
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_locations_name ON admin_locations (lower(name));

CREATE TABLE feature_flags (
    id                   BIGSERIAL PRIMARY KEY,
    feature_name         VARCHAR(100) NOT NULL UNIQUE,
    is_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    rollout_percentage   INT          NOT NULL DEFAULT 0 CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100),
    platform             VARCHAR(16)  NOT NULL DEFAULT 'WEB' CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE admin_reports (
    id            BIGSERIAL PRIMARY KEY,
    reporter_id   VARCHAR(64)  NOT NULL,
    target_type   VARCHAR(32)  NOT NULL,
    target_id     VARCHAR(128) NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    details       TEXT         NOT NULL DEFAULT '',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RESOLVED', 'DISMISSED')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at   TIMESTAMPTZ,
    resolution_note TEXT
);
CREATE INDEX idx_admin_reports_status ON admin_reports (status, created_at DESC);

CREATE TABLE moderation_actions (
    id            BIGSERIAL PRIMARY KEY,
    report_id     BIGINT REFERENCES admin_reports (id) ON DELETE SET NULL,
    actor_id      VARCHAR(64) NOT NULL,
    action_type   VARCHAR(32)  NOT NULL,
    note          TEXT         NOT NULL DEFAULT '',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_moderation_actions_target ON moderation_actions (actor_id, created_at DESC);

CREATE TABLE user_sanctions (
    user_id        VARCHAR(64) PRIMARY KEY,
    banned         BOOLEAN     NOT NULL DEFAULT FALSE,
    restricted     BOOLEAN     NOT NULL DEFAULT FALSE,
    banned_at      TIMESTAMPTZ,
    restricted_at  TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    actor_id      VARCHAR(64)  NOT NULL,
    action        VARCHAR(120) NOT NULL,
    entity_type   VARCHAR(64)  NOT NULL,
    entity_id     VARCHAR(128) NOT NULL,
    before_data   JSONB,
    after_data    JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_id, created_at DESC);

-- First admin user and optional seed data are created in AdminDataInitializer (BCrypt) at startup
