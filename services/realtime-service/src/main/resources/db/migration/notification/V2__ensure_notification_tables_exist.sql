-- Defensive migration: ensures notification schema objects exist even if
-- V1 is already marked applied but tables were dropped/reset manually.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_type') THEN
        CREATE TYPE notification_type AS ENUM (
            'POST_LIKED',
            'POST_COMMENTED',
            'USER_FOLLOWED',
            'DM_RECEIVED',
            'POST_MENTIONED',
            'COMMENT_REPLIED',
            'STORY_VIEWED',
            'REEL_LIKED',
            'REEL_COMMENTED'
        );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_channel') THEN
        CREATE TYPE notification_channel AS ENUM (
            'PUSH',
            'IN_APP',
            'EMAIL',
            'SMS'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type notification_type NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    push_enabled BOOLEAN NOT NULL DEFAULT true,
    in_app_enabled BOOLEAN NOT NULL DEFAULT true,
    email_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, notification_type)
);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id
    ON notification_preferences(user_id);

CREATE TABLE IF NOT EXISTS device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_id VARCHAR(255),
    token TEXT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    app_version VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, device_id, token)
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_device_tokens_token ON device_tokens(token);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type notification_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB,
    actor_id UUID,
    target_id UUID,
    target_type VARCHAR(50),
    read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    pushed BOOLEAN NOT NULL DEFAULT false,
    pushed_at TIMESTAMP WITH TIME ZONE,
    in_app_delivered BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS notification_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id VARCHAR(255) NOT NULL,
    notification_ids UUID[] NOT NULL,
    channel notification_channel NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(batch_id)
);

CREATE INDEX IF NOT EXISTS idx_notification_batches_status ON notification_batches(status);
CREATE INDEX IF NOT EXISTS idx_notification_batches_created_at ON notification_batches(created_at);
