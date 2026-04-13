package com.travelo.messagingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class MessageDto {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("conversation_id")
    private UUID conversationId;
    @JsonProperty("sender_id")
    private UUID senderId;
    @JsonProperty("content")
    private String content;
    @JsonProperty("message_type")
    private String messageType;
    @JsonProperty("attachment_url")
    private String attachmentUrl;
    @JsonProperty("attachment_metadata")
    private Map<String, Object> attachmentMetadata;
    @JsonProperty("reply_to_id")
    private UUID replyToId;
    @JsonProperty("status")
    private String status;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public Map<String, Object> getAttachmentMetadata() { return attachmentMetadata; }
    public void setAttachmentMetadata(Map<String, Object> attachmentMetadata) { this.attachmentMetadata = attachmentMetadata; }
    public UUID getReplyToId() { return replyToId; }
    public void setReplyToId(UUID replyToId) { this.replyToId = replyToId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "MessageDto{" +
                "id=" + id +
                ", conversationId=" + conversationId +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", messageType='" + messageType + '\'' +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                ", attachmentMetadata=" + attachmentMetadata +
                ", replyToId=" + replyToId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

