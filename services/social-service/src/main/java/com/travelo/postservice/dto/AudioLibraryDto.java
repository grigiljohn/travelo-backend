package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AudioLibraryDto(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("file_url") String fileUrl,
    @JsonProperty("duration_seconds") Integer durationSeconds,
    @JsonProperty("category") String category,
    @JsonProperty("thumbnail_url") String thumbnailUrl
) {}

