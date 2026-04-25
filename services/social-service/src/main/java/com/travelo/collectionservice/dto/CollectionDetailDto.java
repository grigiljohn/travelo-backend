package com.travelo.collectionservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.collectionservice.entity.CollectionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CollectionDetailDto(
        UUID id,
        String title,
        CollectionType type,
        @JsonProperty("trip_id") String tripId,
        @JsonProperty("cover_image_url") String coverImageUrl,
        @JsonProperty("media_count") long mediaCount,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("preview_media") List<CollectionMediaDto> previewMedia
) {}
