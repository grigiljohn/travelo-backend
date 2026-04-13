package com.travelo.postservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Post comment entity - tracks comments on posts with support for threaded replies.
 */
@Entity
@Table(name = "post_comments", indexes = {
    @Index(name = "idx_post_comment_post_id", columnList = "post_id"),
    @Index(name = "idx_post_comment_user_id", columnList = "user_id"),
    @Index(name = "idx_post_comment_created_at", columnList = "created_at"),
    @Index(name = "idx_post_comment_parent_id", columnList = "parent_id")
})
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "comment_text", length = 2000, nullable = false)
    private String commentText;

    @Column(name = "parent_id", columnDefinition = "UUID")
    private UUID parentId; // For threaded comments (replies)

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    protected PostComment() {
        // for JPA
    }

    public PostComment(String postId, String userId, String commentText) {
        this.postId = postId;
        this.userId = userId;
        this.commentText = commentText;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
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
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    @Override
    public String toString() {
        return "PostComment{" +
                "id=" + id +
                ", postId='" + postId + '\'' +
                ", userId='" + userId + '\'' +
                ", commentText='" + commentText + '\'' +
                ", parentId=" + parentId +
                ", likeCount=" + likeCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}

