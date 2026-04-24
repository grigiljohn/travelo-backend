-- Communities / circles for GET|POST /api/v1/circles/communities (social-service, travelo_posts DB).

CREATE TABLE circle_communities (
    id                    VARCHAR(64) PRIMARY KEY,
    name                  VARCHAR(200)  NOT NULL,
    description           VARCHAR(4000) NOT NULL DEFAULT '',
    city                  VARCHAR(200)  NOT NULL DEFAULT '',
    visibility            VARCHAR(16)   NOT NULL DEFAULT 'public',
    owner_user_id         VARCHAR(64)   NOT NULL,
    member_count          INT           NOT NULL DEFAULT 1,
    last_activity_label   VARCHAR(64)   NOT NULL DEFAULT 'Just now',
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_circle_communities_visibility CHECK (visibility IN ('public', 'private')),
    CONSTRAINT chk_circle_communities_member_count CHECK (member_count >= 0)
);

CREATE INDEX idx_circle_communities_owner ON circle_communities (owner_user_id);
CREATE INDEX idx_circle_communities_city ON circle_communities (city);
CREATE INDEX idx_circle_communities_visibility ON circle_communities (visibility);

CREATE TABLE circle_community_members (
    community_id VARCHAR(64) NOT NULL REFERENCES circle_communities (id) ON DELETE CASCADE,
    user_id      VARCHAR(64) NOT NULL,
    role         VARCHAR(16)  NOT NULL DEFAULT 'MEMBER',
    joined_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (community_id, user_id),
    CONSTRAINT chk_circle_community_members_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE INDEX idx_circle_community_members_user ON circle_community_members (user_id);

CREATE TABLE circle_community_tags (
    community_id VARCHAR(64) NOT NULL REFERENCES circle_communities (id) ON DELETE CASCADE,
    tag            VARCHAR(48) NOT NULL,
    PRIMARY KEY (community_id, tag)
);

COMMENT ON TABLE circle_communities IS 'Public or private travel circles; member_count denormalized for list cards.';
COMMENT ON COLUMN circle_community_members.role IS 'OWNER optional row; MEMBER for invites and joins.';
COMMENT ON TABLE circle_community_tags IS 'Normalized tags for search / filters.';
