package com.travelo.reelservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrackReelViewRequest {
    
    @JsonProperty("view_duration_seconds")
    private Integer viewDurationSeconds;
    
    @JsonProperty("completion_percentage")
    private Double completionPercentage;

    public Integer getViewDurationSeconds() { return viewDurationSeconds; }
    public void setViewDurationSeconds(Integer viewDurationSeconds) { this.viewDurationSeconds = viewDurationSeconds; }
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
}

