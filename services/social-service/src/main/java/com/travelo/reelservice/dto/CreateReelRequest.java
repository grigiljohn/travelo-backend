package com.travelo.reelservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Request DTO for creating a reel.
 */
public class CreateReelRequest {
    
    @JsonProperty("post_id")
    private String postId; // Reference to post-service post
    
    @JsonProperty("media_id")
    private UUID mediaId; // Media ID from media-service
    
    private String caption;
    private String location;
    
    @JsonProperty("music_track")
    private String musicTrack;

    // Getters and Setters
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID mediaId) { this.mediaId = mediaId; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getMusicTrack() { return musicTrack; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
}

