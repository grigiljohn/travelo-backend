package com.travelo.messagingservice.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}),
    indexes = {
        @Index(name = "idx_conversation_participants_user_id", columnList = "user_id"),
        @Index(name = "idx_conversation_participants_conversation_id", columnList = "conversation_id")
    })
public class ConversationParticipant {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "conversation_id", nullable = false, columnDefinition = "UUID")
    private UUID conversationId;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "last_read_at")
    private OffsetDateTime lastReadAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(OffsetDateTime joinedAt) { this.joinedAt = joinedAt; }
    public OffsetDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(OffsetDateTime leftAt) { this.leftAt = leftAt; }
    public OffsetDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(OffsetDateTime lastReadAt) { this.lastReadAt = lastReadAt; }

    @Override
    public String toString() {
        return "ConversationParticipant{" +
                "id=" + id +
                ", conversationId=" + conversationId +
                ", userId=" + userId +
                ", joinedAt=" + joinedAt +
                ", leftAt=" + leftAt +
                ", lastReadAt=" + lastReadAt +
                '}';
    }
}

