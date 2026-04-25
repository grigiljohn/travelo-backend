package com.travelo.mapservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MapMediaItemDto(
        String id,
        String type, // cluster | media
        int count,
        double lat,
        double lng,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("media_url") String mediaUrl,
        @JsonProperty("collection_id") String collectionId,
        @JsonProperty("collection_name") String collectionName,
        @JsonProperty("captured_at") OffsetDateTime capturedAt
) {}
