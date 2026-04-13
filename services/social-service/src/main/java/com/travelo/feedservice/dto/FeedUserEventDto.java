package com.travelo.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Single user interaction on the home feed (impressions, taps, dwell) for ranking / ML pipelines.
 */
public class FeedUserEventDto {

    @NotBlank
    @JsonProperty("event_type")
    private String eventType;

    @NotBlank
    @JsonProperty("item_type")
    private String itemType;

    @NotBlank
    @JsonProperty("target_id")
    private String targetId;

    @JsonProperty("dwell_ms")
    private Integer dwellMs;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public FeedUserEventDto() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Integer getDwellMs() {
        return dwellMs;
    }

    public void setDwellMs(Integer dwellMs) {
        this.dwellMs = dwellMs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
