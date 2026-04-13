package com.travelo.storyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.storyservice.entity.Story;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Story response.
 */
public class StoryDto {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UUID id;
    
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("user_avatar")
    private String userAvatar;
    
    @JsonProperty("media_id")
    private UUID mediaId;
    
    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image_urls")
    private List<String> imageUrls;
    
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    @JsonProperty("video_url")
    private String videoUrl;
    
    @JsonProperty("media_type")
    private String mediaType;
    
    private String caption;
    private String location;

    @JsonProperty("location_label")
    private String locationLabel;

    @JsonProperty("best_time")
    private String bestTime;

    private String insight;

    @JsonProperty("story_type")
    private String storyType;
    
    @JsonProperty("music_track")
    private String musicTrack;
    
    @JsonProperty("view_count")
    private Integer viewCount;
    
    @JsonProperty("reply_count")
    private Integer replyCount;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("expires_at")
    private OffsetDateTime expiresAt;
    
    @JsonProperty("remaining_ttl_seconds")
    private Long remainingTtlSeconds;
    
    @JsonProperty("is_viewed")
    private Boolean isViewed;
    
    @JsonProperty("is_highlight")
    private Boolean isHighlight;

    public static StoryDto fromEntity(Story story) {
        StoryDto dto = new StoryDto();
        dto.setId(story.getId());
        dto.setUserId(story.getUserId());
        dto.setUserName(story.getUserName());
        dto.setUserAvatar(story.getUserAvatar());
        dto.setMediaId(story.getMediaId());
        dto.setMediaUrl(story.getMediaUrl());
        dto.setImageUrl(story.getMediaUrl());
        dto.setImageUrls(parseImageUrls(story.getImageUrlsJson(), story.getMediaUrl()));
        dto.setThumbnailUrl(story.getThumbnailUrl());
        dto.setVideoUrl(story.getVideoUrl());
        dto.setMediaType(story.getMediaType() != null ? story.getMediaType().name() : null);
        dto.setCaption(story.getCaption());
        dto.setLocation(story.getLocation());
        dto.setLocationLabel(story.getLocation());
        dto.setBestTime(story.getBestTime());
        dto.setInsight(story.getInsight());
        dto.setStoryType(story.getStoryType());
        dto.setMusicTrack(story.getMusicTrack());
        dto.setViewCount(story.getViewCount());
        dto.setReplyCount(story.getReplyCount());
        dto.setCreatedAt(story.getCreatedAt());
        dto.setExpiresAt(story.getExpiresAt());
        dto.setIsHighlight(story.getIsHighlight());
        return dto;
    }

    private static List<String> parseImageUrls(String imageUrlsJson, String fallbackMediaUrl) {
        if (imageUrlsJson != null && !imageUrlsJson.isBlank()) {
            try {
                return OBJECT_MAPPER.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
                // Fallback below
            }
        }
        if (fallbackMediaUrl != null && !fallbackMediaUrl.isBlank()) {
            return List.of(fallbackMediaUrl);
        }
        return Collections.emptyList();
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
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }
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
    public Long getRemainingTtlSeconds() { return remainingTtlSeconds; }
    public void setRemainingTtlSeconds(Long remainingTtlSeconds) { this.remainingTtlSeconds = remainingTtlSeconds; }
    public Boolean getIsViewed() { return isViewed; }
    public void setIsViewed(Boolean isViewed) { this.isViewed = isViewed; }
    public Boolean getIsHighlight() { return isHighlight; }
    public void setIsHighlight(Boolean isHighlight) { this.isHighlight = isHighlight; }
}

