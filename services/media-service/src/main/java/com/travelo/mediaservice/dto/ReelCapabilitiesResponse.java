package com.travelo.mediaservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReelCapabilitiesResponse(
        @JsonProperty("target_resolution") String targetResolution,
        @JsonProperty("target_aspect_ratio") String targetAspectRatio,
        @JsonProperty("processing_target_ms") Integer processingTargetMs,
        @JsonProperty("filters") List<FilterPreset> filters
) {
    public record FilterPreset(
            String id,
            String label,
            String description,
            @JsonProperty("ffmpeg_video_filter") String ffmpegVideoFilter,
            @JsonProperty("music_category") String musicCategory
    ) {}
}
