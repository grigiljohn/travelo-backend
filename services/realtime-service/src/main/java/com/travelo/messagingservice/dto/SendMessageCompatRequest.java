package com.travelo.messagingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Compatibility body for POST /api/v1/messages/send (maps to {@link CreateMessageRequest}).
 */
public class SendMessageCompatRequest {

    @NotNull
    @JsonProperty("conversation_id")
    private UUID conversationId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("message_type")
    private String messageType = "TEXT";

    @JsonProperty("attachment_url")
    private String attachmentUrl;

    @JsonProperty("attachment_metadata")
    private Map<String, Object> attachmentMetadata;

    @JsonProperty("reply_to_id")
    private UUID replyToId;

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public Map<String, Object> getAttachmentMetadata() {
        return attachmentMetadata;
    }

    public void setAttachmentMetadata(Map<String, Object> attachmentMetadata) {
        this.attachmentMetadata = attachmentMetadata;
    }

    public UUID getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(UUID replyToId) {
        this.replyToId = replyToId;
    }
}
