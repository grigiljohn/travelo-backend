-- Fix the message_search trigger function to remove reference to non-existent updated_at column
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

