package com.travelo.mediaservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ModerationQueueItemResponse(
        @JsonProperty("media_id") UUID mediaId,
        @JsonProperty("owner_id") UUID ownerId,
        @JsonProperty("filename") String filename,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("state") String state,
        @JsonProperty("safety_status") String safetyStatus,
        @JsonProperty("storage_key") String storageKey,
        @JsonProperty("meta") Map<String, Object> meta,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {}

