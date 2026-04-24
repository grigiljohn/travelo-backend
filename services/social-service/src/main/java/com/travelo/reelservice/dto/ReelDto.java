package com.travelo.reelservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.reelservice.entity.Reel;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for Reel response.
 */
public class ReelDto {
    private UUID id;
    
    @JsonProperty("post_id")
    private String postId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("media_id")
    private UUID mediaId;
    
    @JsonProperty("video_url")
    private String videoUrl;
    
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    
    private String caption;
    private String location;
    
    @JsonProperty("music_track")
    private String musicTrack;

    @JsonProperty("filter_type")
    private String filterType;

    @JsonProperty("music_enabled")
    private Boolean musicEnabled;
    
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;
    
    @JsonProperty("like_count")
    private Integer likeCount;
    
    @JsonProperty("comment_count")
    private Integer commentCount;
    
    @JsonProperty("share_count")
    private Integer shareCount;
    
    @JsonProperty("view_count")
    private Integer viewCount;
    
    private String status;
    
    @JsonProperty("transcoding_status")
    private String transcodingStatus;
    
    @JsonProperty("is_liked")
    private Boolean isLiked;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public static ReelDto fromEntity(Reel reel) {
        ReelDto dto = new ReelDto();
        dto.setId(reel.getId());
        dto.setPostId(reel.getPostId());
        dto.setUserId(reel.getUserId());
        dto.setMediaId(reel.getMediaId());
        dto.setVideoUrl(reel.getVideoUrl());
        dto.setThumbnailUrl(reel.getThumbnailUrl());
        dto.setCaption(reel.getCaption());
        dto.setLocation(reel.getLocation());
        dto.setMusicTrack(reel.getMusicTrack());
        dto.setFilterType(reel.getFilterType());
        dto.setMusicEnabled(reel.getMusicEnabled());
        dto.setDurationSeconds(reel.getDurationSeconds());
        dto.setLikeCount(reel.getLikeCount());
        dto.setCommentCount(reel.getCommentCount());
        dto.setShareCount(reel.getShareCount());
        dto.setViewCount(reel.getViewCount());
        dto.setStatus(reel.getStatus() != null ? reel.getStatus().name() : null);
        dto.setTranscodingStatus(reel.getTranscodingStatus() != null ? reel.getTranscodingStatus().name() : null);
        dto.setCreatedAt(reel.getCreatedAt());
        return dto;
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
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getMusicTrack() { return musicTrack; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
    public String getFilterType() { return filterType; }
    public void setFilterType(String filterType) { this.filterType = filterType; }
    public Boolean getMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(Boolean musicEnabled) { this.musicEnabled = musicEnabled; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTranscodingStatus() { return transcodingStatus; }
    public void setTranscodingStatus(String transcodingStatus) { this.transcodingStatus = transcodingStatus; }
    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

