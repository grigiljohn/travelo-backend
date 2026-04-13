package com.travelo.reelservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reel like entity - tracks likes on reels separately from posts.
 */
@Entity
@Table(name = "reel_likes", indexes = {
    @Index(name = "idx_reel_like_reel_id", columnList = "reel_id"),
    @Index(name = "idx_reel_like_user_id", columnList = "user_id"),
    @Index(name = "idx_reel_like_unique", columnList = "reel_id,user_id", unique = true)
})
public class ReelLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reel_id", nullable = false, columnDefinition = "UUID")
    private UUID reelId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "liked_at", nullable = false, updatable = false)
    private OffsetDateTime likedAt;

    @PrePersist
    void onCreate() {
        this.likedAt = OffsetDateTime.now();
    }

    protected ReelLike() {
        // for JPA
    }

    public ReelLike(UUID reelId, String userId) {
        this.reelId = reelId;
        this.userId = userId;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getReelId() { return reelId; }
    public void setReelId(UUID reelId) { this.reelId = reelId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public OffsetDateTime getLikedAt() { return likedAt; }
    public void setLikedAt(OffsetDateTime likedAt) { this.likedAt = likedAt; }

    @Override
    public String toString() {
        return "ReelLike{" +
                "id=" + id +
                ", reelId=" + reelId +
                ", userId='" + userId + '\'' +
                ", likedAt=" + likedAt +
                '}';
    }
}

