-- Create conversation_types enum
CREATE TYPE conversation_type AS ENUM (
    'DIRECT',
    'GROUP'
);

-- Create message_status enum
CREATE TYPE message_status AS ENUM (
    'SENT',
    'DELIVERED',
    'READ'
);

-- Create conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type conversation_type NOT NULL DEFAULT 'DIRECT',
    name VARCHAR(255), -- For group chats
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_message_at TIMESTAMP WITH TIME ZONE,
    last_message_id UUID,
    encrypted BOOLEAN NOT NULL DEFAULT false -- E2E encryption flag
);

CREATE INDEX idx_conversations_created_by ON conversations(created_by);
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at DESC);

-- Create conversation_participants table
CREATE TABLE conversation_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    left_at TIMESTAMP WITH TIME ZONE,
    last_read_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_user_id ON conversation_participants(user_id);
CREATE INDEX idx_conversation_participants_conversation_id ON conversation_participants(conversation_id);

-- Create messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT', -- 'TEXT', 'IMAGE', 'VIDEO', 'FILE', 'AUDIO'
    attachment_url TEXT,
    attachment_metadata JSONB,
    reply_to_id UUID REFERENCES messages(id), -- For threading
    status message_status NOT NULL DEFAULT 'SENT',
    encrypted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_created_at ON messages(conversation_id, created_at DESC);
CREATE INDEX idx_messages_reply_to_id ON messages(reply_to_id);

-- Create message_read_receipts table
CREATE TABLE message_read_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(message_id, user_id)
);

CREATE INDEX idx_message_read_receipts_user_id ON message_read_receipts(user_id);
CREATE INDEX idx_message_read_receipts_message_id ON message_read_receipts(message_id);

-- Create message_search table for full-text search (can use PostgreSQL's tsvector or external search)
-- This is a denormalized table for search purposes
CREATE TABLE message_search (
    message_id UUID PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content_text TEXT NOT NULL,
    search_vector tsvector,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_message_search_vector ON message_search USING gin(search_vector);
CREATE INDEX idx_message_search_conversation_id ON message_search(conversation_id);

-- Trigger function to update search vector
CREATE OR REPLACE FUNCTION update_message_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO message_search (message_id, conversation_id, sender_id, content_text, search_vector, created_at)
    VALUES (NEW.id, NEW.conversation_id, NEW.sender_id, NEW.content, to_tsvector('english', NEW.content), NEW.created_at)
    ON CONFLICT (message_id) DO UPDATE
    SET content_text = NEW.content,
        search_vector = to_tsvector('english', NEW.content);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update search table
CREATE TRIGGER trigger_update_message_search
AFTER INSERT OR UPDATE ON messages
FOR EACH ROW
WHEN (NEW.deleted_at IS NULL)
EXECUTE FUNCTION update_message_search_vector();

