package com.travelo.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Request DTO for marking posts as seen.
 */
public class MarkSeenRequest {
    
    @JsonProperty("surface")
    private String surface;
    
    @JsonProperty("post_ids")
    private Set<String> postIds;

    public MarkSeenRequest() {
    }

    public MarkSeenRequest(String surface, Set<String> postIds) {
        this.surface = surface;
        this.postIds = postIds;
    }

    public String getSurface() {
        return surface;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public Set<String> getPostIds() {
        return postIds;
    }

    public void setPostIds(Set<String> postIds) {
        this.postIds = postIds;
    }
}

