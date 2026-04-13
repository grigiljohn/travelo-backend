package com.travelo.reelservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reel view entity - tracks views for analytics.
 */
@Entity
@Table(name = "reel_views", indexes = {
    @Index(name = "idx_reel_view_reel_id", columnList = "reel_id"),
    @Index(name = "idx_reel_view_user_id", columnList = "user_id"),
    @Index(name = "idx_reel_view_created_at", columnList = "viewed_at")
})
public class ReelView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reel_id", nullable = false, columnDefinition = "UUID")
    private UUID reelId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "view_duration_seconds")
    private Integer viewDurationSeconds; // How long user watched

    @Column(name = "completion_percentage")
    private Double completionPercentage; // 0-100

    @Column(name = "viewed_at", nullable = false, updatable = false)
    private OffsetDateTime viewedAt;

    @PrePersist
    void onCreate() {
        this.viewedAt = OffsetDateTime.now();
    }

    protected ReelView() {
        // for JPA
    }

    public ReelView(UUID reelId, String userId, Integer viewDurationSeconds, Double completionPercentage) {
        this.reelId = reelId;
        this.userId = userId;
        this.viewDurationSeconds = viewDurationSeconds;
        this.completionPercentage = completionPercentage;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getReelId() { return reelId; }
    public void setReelId(UUID reelId) { this.reelId = reelId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getViewDurationSeconds() { return viewDurationSeconds; }
    public void setViewDurationSeconds(Integer viewDurationSeconds) { this.viewDurationSeconds = viewDurationSeconds; }
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    public OffsetDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(OffsetDateTime viewedAt) { this.viewedAt = viewedAt; }

    @Override
    public String toString() {
        return "ReelView{" +
                "id=" + id +
                ", reelId=" + reelId +
                ", userId='" + userId + '\'' +
                ", viewDurationSeconds=" + viewDurationSeconds +
                ", completionPercentage=" + completionPercentage +
                ", viewedAt=" + viewedAt +
                '}';
    }
}

