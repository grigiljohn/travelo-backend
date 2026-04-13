package com.travelo.messagingservice.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_read_receipts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}),
    indexes = {
        @Index(name = "idx_message_read_receipts_user_id", columnList = "user_id"),
        @Index(name = "idx_message_read_receipts_message_id", columnList = "message_id")
    })
public class MessageReadReceipt {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "message_id", nullable = false, columnDefinition = "UUID")
    private UUID messageId;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "read_at", nullable = false, updatable = false)
    private OffsetDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (readAt == null) {
            readAt = OffsetDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }

    @Override
    public String toString() {
        return "MessageReadReceipt{" +
                "id=" + id +
                ", messageId=" + messageId +
                ", userId=" + userId +
                ", readAt=" + readAt +
                '}';
    }
}

