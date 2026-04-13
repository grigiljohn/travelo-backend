package com.travelo.reelservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.reelservice.entity.ReelComment;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReelCommentDto {
    private UUID id;
    
    @JsonProperty("reel_id")
    private UUID reelId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("comment_text")
    private String commentText;
    
    @JsonProperty("parent_id")
    private UUID parentId;
    
    @JsonProperty("like_count")
    private Integer likeCount;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public static ReelCommentDto fromEntity(ReelComment comment) {
        ReelCommentDto dto = new ReelCommentDto();
        dto.setId(comment.getId());
        dto.setReelId(comment.getReelId());
        dto.setUserId(comment.getUserId());
        dto.setCommentText(comment.getCommentText());
        dto.setParentId(comment.getParentId());
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getReelId() { return reelId; }
    public void setReelId(UUID reelId) { this.reelId = reelId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

