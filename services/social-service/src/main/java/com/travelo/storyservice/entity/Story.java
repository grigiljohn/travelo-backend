package com.travelo.storyservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Story entity - ephemeral content with 24h TTL.
 */
@Entity
@Table(name = "stories", indexes = {
    @Index(name = "idx_story_user_id", columnList = "user_id"),
    @Index(name = "idx_story_created_at", columnList = "created_at"),
    @Index(name = "idx_story_expires_at", columnList = "expires_at")
})
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "user_name", length = 120)
    private String userName;

    @Column(name = "user_avatar", length = 500)
    private String userAvatar;

    @Column(name = "media_id", columnDefinition = "UUID")
    private UUID mediaId; // Reference to media-service media ID

    @Column(name = "media_url", length = 500)
    private String mediaUrl; // Cached URL

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "image_urls_json", columnDefinition = "TEXT")
    private String imageUrlsJson;

    @Column(name = "media_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @Column(length = 500)
    private String caption;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "best_time", length = 120)
    private String bestTime;

    @Column(name = "insight", length = 500)
    private String insight;

    @Column(name = "story_type", length = 20)
    private String storyType;

    @Column(name = "music_track", length = 255)
    private String musicTrack;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "reply_count", nullable = false)
    private Integer replyCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_highlight", nullable = false)
    private Boolean isHighlight = false;

    @Column(name = "highlight_id", columnDefinition = "UUID")
    private UUID highlightId; // If saved to a highlight

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.expiresAt == null) {
            // Default: 24 hours from creation
            this.expiresAt = this.createdAt.plusHours(24);
        }
    }

    protected Story() {
        // for JPA
    }

    public Story(String userId, UUID mediaId, String mediaUrl, String thumbnailUrl, 
                 MediaType mediaType, String caption) {
        this.userId = userId;
        this.mediaId = mediaId;
        this.mediaUrl = mediaUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaType = mediaType;
        this.caption = caption;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID mediaId) { this.mediaId = mediaId; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getImageUrlsJson() { return imageUrlsJson; }
    public void setImageUrlsJson(String imageUrlsJson) { this.imageUrlsJson = imageUrlsJson; }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getBestTime() { return bestTime; }
    public void setBestTime(String bestTime) { this.bestTime = bestTime; }
    public String getInsight() { return insight; }
    public void setInsight(String insight) { this.insight = insight; }
    public String getStoryType() { return storyType; }
    public void setStoryType(String storyType) { this.storyType = storyType; }
    public String getMusicTrack() { return musicTrack; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getIsHighlight() { return isHighlight; }
    public void setIsHighlight(Boolean isHighlight) { this.isHighlight = isHighlight; }
    public UUID getHighlightId() { return highlightId; }
    public void setHighlightId(UUID highlightId) { this.highlightId = highlightId; }

    public enum MediaType {
        IMAGE, VIDEO
    }

    @Override
    public String toString() {
        return "Story{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", mediaId=" + mediaId +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", mediaType=" + mediaType +
                ", caption='" + caption + '\'' +
                ", location='" + location + '\'' +
                ", bestTime='" + bestTime + '\'' +
                ", insight='" + insight + '\'' +
                ", storyType='" + storyType + '\'' +
                ", musicTrack='" + musicTrack + '\'' +
                ", viewCount=" + viewCount +
                ", replyCount=" + replyCount +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", isHighlight=" + isHighlight +
                ", highlightId=" + highlightId +
                '}';
    }
}

