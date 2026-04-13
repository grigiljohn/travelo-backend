package com.travelo.messagingservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversations_created_by", columnList = "created_by"),
    @Index(name = "idx_conversations_updated_at", columnList = "updated_at")
})
public class Conversation {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "conversation_type")
    private ConversationType type = ConversationType.DIRECT;

    @Column(length = 255)
    private String name; // For group chats

    @Column(name = "created_by", nullable = false, columnDefinition = "UUID")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Column(name = "last_message_id", columnDefinition = "UUID")
    private UUID lastMessageId;

    @Column(nullable = false)
    private Boolean encrypted = false; // E2E encryption flag

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ConversationType getType() { return type; }
    public void setType(ConversationType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(OffsetDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public UUID getLastMessageId() { return lastMessageId; }
    public void setLastMessageId(UUID lastMessageId) { this.lastMessageId = lastMessageId; }
    public Boolean getEncrypted() { return encrypted; }
    public void setEncrypted(Boolean encrypted) { this.encrypted = encrypted; }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastMessageAt=" + lastMessageAt +
                ", lastMessageId=" + lastMessageId +
                ", encrypted=" + encrypted +
                '}';
    }
}

