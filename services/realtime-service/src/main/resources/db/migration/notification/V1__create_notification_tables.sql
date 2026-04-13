-- Create notification_types enum
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

-- Create notification_channels enum
CREATE TYPE notification_channel AS ENUM (
    'PUSH',
    'IN_APP',
    'EMAIL',
    'SMS'
);

-- Create notification_preferences table
CREATE TABLE notification_preferences (
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

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

-- Create device_tokens table
CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_id VARCHAR(255),
    token TEXT NOT NULL,
    platform VARCHAR(50) NOT NULL, -- 'IOS', 'ANDROID', 'WEB'
    app_version VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, device_id, token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX idx_device_tokens_token ON device_tokens(token);

-- Create notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- recipient
    notification_type notification_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB, -- Additional metadata
    actor_id UUID, -- User who triggered the notification
    target_id UUID, -- Target entity (post_id, comment_id, etc.)
    target_type VARCHAR(50), -- 'POST', 'COMMENT', 'USER', 'MESSAGE'
    read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    pushed BOOLEAN NOT NULL DEFAULT false,
    pushed_at TIMESTAMP WITH TIME ZONE,
    in_app_delivered BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

-- Create notification_batches table for batch delivery tracking
CREATE TABLE notification_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id VARCHAR(255) NOT NULL,
    notification_ids UUID[] NOT NULL,
    channel notification_channel NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'FAILED'
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(batch_id)
);

CREATE INDEX idx_notification_batches_status ON notification_batches(status);
CREATE INDEX idx_notification_batches_created_at ON notification_batches(created_at);

