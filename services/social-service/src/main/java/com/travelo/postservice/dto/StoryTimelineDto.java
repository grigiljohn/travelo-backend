package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record StoryTimelineDto(
    @JsonProperty("id") String id,
    @JsonProperty("user_id") String userId,
    @JsonProperty("media_order") List<String> mediaOrder,
    @JsonProperty("durations") Map<String, Integer> durations,
    @JsonProperty("transitions") List<Map<String, Object>> transitions,
    @JsonProperty("text_overlays") List<Map<String, Object>> textOverlays,
    @JsonProperty("template_id") String templateId,
    @JsonProperty("created_at") OffsetDateTime createdAt
) {}

