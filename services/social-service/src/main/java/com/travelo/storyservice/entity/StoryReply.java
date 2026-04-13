package com.travelo.storyservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Story reply entity - allows users to reply to stories.
 */
@Entity
@Table(name = "story_replies", indexes = {
    @Index(name = "idx_story_reply_story_id", columnList = "story_id"),
    @Index(name = "idx_story_reply_user_id", columnList = "user_id"),
    @Index(name = "idx_story_reply_created_at", columnList = "created_at")
})
public class StoryReply {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "story_id", nullable = false, columnDefinition = "UUID")
    private UUID storyId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "reply_text", length = 500, nullable = false)
    private String replyText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected StoryReply() {
        // for JPA
    }

    public StoryReply(UUID storyId, String userId, String replyText) {
        this.storyId = storyId;
        this.userId = userId;
        this.replyText = replyText;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStoryId() { return storyId; }
    public void setStoryId(UUID storyId) { this.storyId = storyId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "StoryReply{" +
                "id=" + id +
                ", storyId=" + storyId +
                ", userId='" + userId + '\'' +
                ", replyText='" + replyText + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

