package com.travelo.userservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "follows", indexes = {
    @Index(name = "idx_follower_followee", columnList = "follower_id,followee_id", unique = true),
    @Index(name = "idx_follower_id", columnList = "follower_id"),
    @Index(name = "idx_followee_id", columnList = "followee_id")
})
public class Follow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "follower_id", nullable = false)
    private UUID followerId;
    
    @Column(name = "followee_id", nullable = false)
    private UUID followeeId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public Follow() {
    }

    public Follow(UUID followerId, UUID followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public void setFollowerId(UUID followerId) {
        this.followerId = followerId;
    }

    public UUID getFolloweeId() {
        return followeeId;
    }

    public void setFolloweeId(UUID followeeId) {
        this.followeeId = followeeId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

