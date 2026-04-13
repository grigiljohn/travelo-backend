package com.travelo.reelservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class CreateReelCommentRequest {
    
    @JsonProperty("comment_text")
    private String commentText;
    
    @JsonProperty("parent_id")
    private UUID parentId; // For threaded comments

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
}

