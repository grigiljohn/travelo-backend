package com.travelo.storyservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a story highlight.
 */
public class CreateStoryHighlightRequest {
    
    private String title;
    
    @JsonProperty("cover_image_url")
    private String coverImageUrl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
}

