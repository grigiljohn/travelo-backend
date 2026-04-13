package com.travelo.adservice.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Response DTO from media-service
 * This should match the MediaFileResponse from media-service
 */
public record MediaFileResponse(
        Long id,
        @JsonProperty("fileKey") String fileKey,
        @JsonProperty("fileUrl") String fileUrl,
        @JsonProperty("mediaType") String mediaType,
        @JsonProperty("status") String status,
        @JsonProperty("moderationReason") String moderationReason,
        @JsonProperty("createdAt") OffsetDateTime createdAt,
        @JsonProperty("updatedAt") OffsetDateTime updatedAt
) {
}

