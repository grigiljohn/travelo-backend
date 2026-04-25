package com.travelo.collectionservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.collectionservice.entity.CollectionMediaSourceType;
import com.travelo.collectionservice.entity.CollectionMediaType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CollectionMediaDto(
        UUID id,
        @JsonProperty("media_url") String mediaUrl,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("media_type") CollectionMediaType mediaType,
        @JsonProperty("source_type") CollectionMediaSourceType sourceType,
        @JsonProperty("source_id") String sourceId,
        @JsonProperty("captured_at") OffsetDateTime capturedAt,
        Double latitude,
        Double longitude,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {}
