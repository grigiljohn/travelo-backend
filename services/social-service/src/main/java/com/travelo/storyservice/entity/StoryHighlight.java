package com.travelo.storyservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Story highlight entity - permanent collections of stories.
 */
@Entity
@Table(name = "story_highlights", indexes = {
    @Index(name = "idx_story_highlight_user_id", columnList = "user_id"),
    @Index(name = "idx_story_highlight_created_at", columnList = "created_at")
})
public class StoryHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "story_count", nullable = false)
    private Integer storyCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    protected StoryHighlight() {
        // for JPA
    }

    public StoryHighlight(String userId, String title, String coverImageUrl) {
        this.userId = userId;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public Integer getStoryCount() { return storyCount; }
    public void setStoryCount(Integer storyCount) { this.storyCount = storyCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "StoryHighlight{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", coverImageUrl='" + coverImageUrl + '\'' +
                ", storyCount=" + storyCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

