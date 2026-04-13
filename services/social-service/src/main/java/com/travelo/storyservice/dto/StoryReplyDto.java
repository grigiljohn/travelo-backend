package com.travelo.storyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.storyservice.entity.StoryReply;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for story reply.
 */
public class StoryReplyDto {
    private UUID id;
    
    @JsonProperty("story_id")
    private UUID storyId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("reply_text")
    private String replyText;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public static StoryReplyDto fromEntity(StoryReply reply) {
        StoryReplyDto dto = new StoryReplyDto();
        dto.setId(reply.getId());
        dto.setStoryId(reply.getStoryId());
        dto.setUserId(reply.getUserId());
        dto.setReplyText(reply.getReplyText());
        dto.setCreatedAt(reply.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStoryId() { return storyId; }
    public void setStoryId(UUID storyId) { this.storyId = storyId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

