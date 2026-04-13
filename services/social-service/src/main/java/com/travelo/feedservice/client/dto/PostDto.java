package com.travelo.feedservice.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO for Post from post-service.
 * Mirrors the structure from post-service.
 */
public class PostDto {
    private String id;
    
    @JsonProperty("user_id")
    private String userId;
    
    private String username;
    
    @JsonProperty("user_avatar")
    private String userAvatar;
    
    @JsonProperty("post_type")
    private String postType;
    
    private String content;
    private List<String> images;
    
    @JsonProperty("media_items")
    private List<MediaItemDto> mediaItems;
    
    private String caption;
    private List<String> tags;
    private String mood;
    private String location;
    private Integer likes;
    private Integer comments;
    private Integer remixes;
    private Integer tips;
    private Integer shares;
    
    @JsonProperty("is_liked")
    private Boolean isLiked;
    
    @JsonProperty("is_following")
    private Boolean isFollowing;
    
    @JsonProperty("is_saved")
    private Boolean isSaved;
    
    @JsonProperty("is_verified")
    private Boolean isVerified;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
    
    private Integer duration;
    
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    
    @JsonProperty("music_track")
    private String musicTrack;
    
    private Map<String, Object> metadata;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public List<MediaItemDto> getMediaItems() { return mediaItems; }
    public void setMediaItems(List<MediaItemDto> mediaItems) { this.mediaItems = mediaItems; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public Integer getComments() { return comments; }
    public void setComments(Integer comments) { this.comments = comments; }
    public Integer getRemixes() { return remixes; }
    public void setRemixes(Integer remixes) { this.remixes = remixes; }
    public Integer getTips() { return tips; }
    public void setTips(Integer tips) { this.tips = tips; }
    public Integer getShares() { return shares; }
    public void setShares(Integer shares) { this.shares = shares; }
    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
    public Boolean getIsSaved() { return isSaved; }
    public void setIsSaved(Boolean isSaved) { this.isSaved = isSaved; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getMusicTrack() { return musicTrack; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public static class MediaItemDto {
        private String id;
        private String url;
        private String type;
        private Integer position;
        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
        private Integer duration;
        private Integer width;
        private Integer height;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
    }
}

