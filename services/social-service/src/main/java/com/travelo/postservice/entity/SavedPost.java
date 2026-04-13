package com.travelo.postservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Saved post entity - tracks posts saved by users to collections.
 */
@Entity
@Table(name = "saved_posts", indexes = {
    @Index(name = "idx_saved_posts_user_id", columnList = "user_id"),
    @Index(name = "idx_saved_posts_post_id", columnList = "post_id"),
    @Index(name = "idx_saved_posts_collection", columnList = "user_id,collection_name"),
    @Index(name = "idx_saved_posts_created_at", columnList = "created_at")
},
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "post_id", "collection_name"})
})
public class SavedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @Column(name = "collection_name", length = 100, nullable = false)
    private String collectionName = "All Posts";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected SavedPost() {
        // for JPA
    }

    public SavedPost(String userId, String postId, String collectionName) {
        this.userId = userId;
        this.postId = postId;
        this.collectionName = collectionName != null ? collectionName : "All Posts";
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    @Override
    public String toString() {
        return "SavedPost{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", postId='" + postId + '\'' +
                ", collectionName='" + collectionName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

