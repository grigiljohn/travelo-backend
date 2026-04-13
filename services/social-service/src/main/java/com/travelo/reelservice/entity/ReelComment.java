package com.travelo.reelservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reel comment entity - tracks comments on reels separately from posts.
 */
@Entity
@Table(name = "reel_comments", indexes = {
    @Index(name = "idx_reel_comment_reel_id", columnList = "reel_id"),
    @Index(name = "idx_reel_comment_user_id", columnList = "user_id"),
    @Index(name = "idx_reel_comment_created_at", columnList = "created_at"),
    @Index(name = "idx_reel_comment_parent_id", columnList = "parent_id")
})
public class ReelComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reel_id", nullable = false, columnDefinition = "UUID")
    private UUID reelId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "comment_text", length = 1000, nullable = false)
    private String commentText;

    @Column(name = "parent_id", columnDefinition = "UUID")
    private UUID parentId; // For threaded comments

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

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

    protected ReelComment() {
        // for JPA
    }

    public ReelComment(UUID reelId, String userId, String commentText) {
        this.reelId = reelId;
        this.userId = userId;
        this.commentText = commentText;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getReelId() { return reelId; }
    public void setReelId(UUID reelId) { this.reelId = reelId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ReelComment{" +
                "id=" + id +
                ", reelId=" + reelId +
                ", userId='" + userId + '\'' +
                ", commentText='" + commentText + '\'' +
                ", parentId=" + parentId +
                ", likeCount=" + likeCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

