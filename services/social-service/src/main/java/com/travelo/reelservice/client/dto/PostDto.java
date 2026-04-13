package com.travelo.reelservice.client.dto;

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
    
    @JsonProperty("created_at")
    private String createdAt;
    
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
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getMusicTrack() { return musicTrack; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
    public List<MediaItemDto> getMediaItems() { return mediaItems; }
    public void setMediaItems(List<MediaItemDto> mediaItems) { this.mediaItems = mediaItems; }
    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

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
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
    }
}

