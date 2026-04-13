package com.travelo.storyservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Story view entity - tracks who viewed which story.
 */
@Entity
@Table(name = "story_views", indexes = {
    @Index(name = "idx_story_view_story_id", columnList = "story_id"),
    @Index(name = "idx_story_view_user_id", columnList = "user_id"),
    @Index(name = "idx_story_view_unique", columnList = "story_id,user_id", unique = true)
})
public class StoryView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "story_id", nullable = false, columnDefinition = "UUID")
    private UUID storyId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "viewed_at", nullable = false, updatable = false)
    private OffsetDateTime viewedAt;

    @PrePersist
    void onCreate() {
        this.viewedAt = OffsetDateTime.now();
    }

    protected StoryView() {
        // for JPA
    }

    public StoryView(UUID storyId, String userId) {
        this.storyId = storyId;
        this.userId = userId;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStoryId() { return storyId; }
    public void setStoryId(UUID storyId) { this.storyId = storyId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public OffsetDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(OffsetDateTime viewedAt) { this.viewedAt = viewedAt; }

    @Override
    public String toString() {
        return "StoryView{" +
                "id=" + id +
                ", storyId=" + storyId +
                ", userId='" + userId + '\'' +
                ", viewedAt=" + viewedAt +
                '}';
    }
}

