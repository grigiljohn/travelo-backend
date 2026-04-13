package com.travelo.reelservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reel entity - short video content with separate tracking.
 */
@Entity
@Table(name = "reels", indexes = {
    @Index(name = "idx_reel_user_id", columnList = "user_id"),
    @Index(name = "idx_reel_created_at", columnList = "created_at"),
    @Index(name = "idx_reel_post_id", columnList = "post_id"),
    @Index(name = "idx_reel_status", columnList = "status")
})
public class Reel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_id", length = 50, unique = true)
    private String postId; // Reference to post-service post

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "media_id", columnDefinition = "UUID")
    private UUID mediaId; // Reference to media-service media ID

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(length = 1000)
    private String caption;

    @Column(name = "music_track", length = 255)
    private String musicTrack;

    @Column(length = 255)
    private String location;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "share_count", nullable = false)
    private Integer shareCount = 0;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "transcoding_status", length = 20)
    @Enumerated(EnumType.STRING)
    private TranscodingStatus transcodingStatus = TranscodingStatus.PENDING;

    @Column(name = "ranking_score")
    private Double rankingScore; // ML-based ranking score

    @Column(name = "recommendation_score")
    private Double recommendationScore; // ML recommendation score

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

    protected Reel() {
        // for JPA
    }

    public Reel(String postId, String userId, UUID mediaId, String videoUrl, String thumbnailUrl, String caption) {
        this.postId = postId;
        this.userId = userId;
        this.mediaId = mediaId;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.caption = caption;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID mediaId) { this.mediaId = mediaId; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getMusicTrack() { return musicTrack; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public TranscodingStatus getTranscodingStatus() { return transcodingStatus; }
    public void setTranscodingStatus(TranscodingStatus transcodingStatus) { this.transcodingStatus = transcodingStatus; }
    public Double getRankingScore() { return rankingScore; }
    public void setRankingScore(Double rankingScore) { this.rankingScore = rankingScore; }
    public Double getRecommendationScore() { return recommendationScore; }
    public void setRecommendationScore(Double recommendationScore) { this.recommendationScore = recommendationScore; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum Status {
        PENDING, PROCESSING, READY, FAILED
    }

    public enum TranscodingStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    @Override
    public String toString() {
        return "Reel{" +
                "id=" + id +
                ", postId='" + postId + '\'' +
                ", userId='" + userId + '\'' +
                ", mediaId=" + mediaId +
                ", videoUrl='" + videoUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", caption='" + caption + '\'' +
                ", musicTrack='" + musicTrack + '\'' +
                ", location='" + location + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", shareCount=" + shareCount +
                ", viewCount=" + viewCount +
                ", status=" + status +
                ", transcodingStatus=" + transcodingStatus +
                ", rankingScore=" + rankingScore +
                ", recommendationScore=" + recommendationScore +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

