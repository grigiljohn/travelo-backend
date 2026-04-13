package com.travelo.storyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a story reply.
 */
public class CreateStoryReplyRequest {
    
    @JsonProperty("reply_text")
    private String replyText;

    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
}

