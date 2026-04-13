package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record GenerateTimelineRequest(
    @NotEmpty(message = "Media files are required")
    @JsonProperty("media_files") List<String> mediaFiles, // List of media IDs or URLs
    @JsonProperty("user_preferences") Map<String, Object> userPreferences,
    @JsonProperty("template_id") String templateId
) {}

