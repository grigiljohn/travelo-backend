package com.travelo.collectionservice.dto;

import com.travelo.collectionservice.entity.CollectionMediaSourceType;
import com.travelo.collectionservice.entity.CollectionMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record AddCollectionMediaItemRequest(
        @NotBlank @Size(max = 800) String mediaUrl,
        @Size(max = 800) String thumbnailUrl,
        CollectionMediaType mediaType,
        CollectionMediaSourceType sourceType,
        @Size(max = 120) String sourceId,
        OffsetDateTime capturedAt,
        Double latitude,
        Double longitude
) {}
