-- Extended community profile, secret visibility, join approval, structured topics.

ALTER TABLE circle_communities DROP CONSTRAINT IF EXISTS chk_circle_communities_visibility;
ALTER TABLE circle_communities ADD CONSTRAINT chk_circle_communities_visibility
    CHECK (visibility IN ('public', 'private', 'secret'));

ALTER TABLE circle_communities ADD COLUMN IF NOT EXISTS tagline VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE circle_communities ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(2048);
ALTER TABLE circle_communities ADD COLUMN IF NOT EXISTS icon_image_url VARCHAR(2048);
ALTER TABLE circle_communities ADD COLUMN IF NOT EXISTS rules_text TEXT NOT NULL DEFAULT '';
ALTER TABLE circle_communities ADD COLUMN IF NOT EXISTS topics_json TEXT NOT NULL DEFAULT '[]';
ALTER TABLE circle_communities ADD COLUMN IF NOT EXISTS require_admin_approval BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE circle_communities ADD COLUMN IF NOT EXISTS allow_member_invites BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE circle_community_members ADD COLUMN IF NOT EXISTS membership_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE circle_community_members DROP CONSTRAINT IF EXISTS chk_circle_community_members_status;
ALTER TABLE circle_community_members ADD CONSTRAINT chk_circle_community_members_status
    CHECK (membership_status IN ('ACTIVE', 'PENDING'));

COMMENT ON COLUMN circle_communities.topics_json IS 'JSON array of channel/topic names.';
COMMENT ON COLUMN circle_community_members.membership_status IS 'ACTIVE member or PENDING join request (public + require_admin_approval).';
