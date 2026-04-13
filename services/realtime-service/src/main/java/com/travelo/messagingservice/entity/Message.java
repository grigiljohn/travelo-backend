package com.travelo.messagingservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_messages_sender_id", columnList = "sender_id"),
    @Index(name = "idx_messages_created_at", columnList = "conversation_id, created_at"),
    @Index(name = "idx_messages_reply_to_id", columnList = "reply_to_id")
})
public class Message {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "conversation_id", nullable = false, columnDefinition = "UUID")
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false, columnDefinition = "UUID")
    private UUID senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType = "TEXT"; // 'TEXT', 'IMAGE', 'VIDEO', 'FILE', 'AUDIO'

    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachment_metadata", columnDefinition = "JSONB")
    private Map<String, Object> attachmentMetadata;

    @Column(name = "reply_to_id", columnDefinition = "UUID")
    private UUID replyToId; // For threading

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "message_status")
    private MessageStatus status = MessageStatus.SENT;

    @Column(nullable = false)
    private Boolean encrypted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

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
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    public Boolean getEncrypted() { return encrypted; }
    public void setEncrypted(Boolean encrypted) { this.encrypted = encrypted; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", conversationId=" + conversationId +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", messageType='" + messageType + '\'' +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                ", attachmentMetadata=" + attachmentMetadata +
                ", replyToId=" + replyToId +
                ", status=" + status +
                ", encrypted=" + encrypted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}

