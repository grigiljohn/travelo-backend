package com.travelo.storyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.storyservice.entity.StoryHighlight;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for story highlight.
 */
public class StoryHighlightDto {
    private UUID id;
    
    @JsonProperty("user_id")
    private String userId;
    
    private String title;
    
    @JsonProperty("cover_image_url")
    private String coverImageUrl;
    
    @JsonProperty("story_count")
    private Integer storyCount;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public static StoryHighlightDto fromEntity(StoryHighlight highlight) {
        StoryHighlightDto dto = new StoryHighlightDto();
        dto.setId(highlight.getId());
        dto.setUserId(highlight.getUserId());
        dto.setTitle(highlight.getTitle());
        dto.setCoverImageUrl(highlight.getCoverImageUrl());
        dto.setStoryCount(highlight.getStoryCount());
        dto.setCreatedAt(highlight.getCreatedAt());
        dto.setUpdatedAt(highlight.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public Integer getStoryCount() { return storyCount; }
    public void setStoryCount(Integer storyCount) { this.storyCount = storyCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

