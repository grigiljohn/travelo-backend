package com.travelo.storyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.storyservice.entity.Story;
import java.util.UUID;

/**
 * Request DTO for creating a story.
 */
public class CreateStoryRequest {

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("user_avatar")
    private String userAvatar;

    @JsonProperty("media_id")
    private UUID mediaId;

    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("video_url")
    private String videoUrl;

    @JsonProperty("image_urls_json")
    private String imageUrlsJson;

    @JsonProperty("media_type")
    private Story.MediaType mediaType;

    private String caption;
    private String location;

    @JsonProperty("best_time")
    private String bestTime;

    private String insight;

    @JsonProperty("story_type")
    private String storyType;

    @JsonProperty("music_track")
    private String musicTrack;

    // Getters and Setters
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID mediaId) { this.mediaId = mediaId; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getImageUrlsJson() { return imageUrlsJson; }
    public void setImageUrlsJson(String imageUrlsJson) { this.imageUrlsJson = imageUrlsJson; }
    public Story.MediaType getMediaType() { return mediaType; }
    public void setMediaType(Story.MediaType mediaType) { this.mediaType = mediaType; }
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
}

