package com.travelo.storyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.storyservice.entity.StoryView;

import java.time.OffsetDateTime;

/**
 * DTO for story viewer information.
 */
public class StoryViewerDto {
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("viewed_at")
    private OffsetDateTime viewedAt;

    public static StoryViewerDto fromEntity(StoryView view) {
        StoryViewerDto dto = new StoryViewerDto();
        dto.setUserId(view.getUserId());
        dto.setViewedAt(view.getViewedAt());
        return dto;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public OffsetDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(OffsetDateTime viewedAt) { this.viewedAt = viewedAt; }
}

