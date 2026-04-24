package com.travelo.reelservice.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ReelProcessMediaResponse(
        @JsonProperty("media_id") UUID mediaId,
        @JsonProperty("download_url") String downloadUrl,
        @JsonProperty("storage_key") String storageKey,
        @JsonProperty("duration_seconds") Integer durationSeconds,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("fallback_original") boolean fallbackOriginal
) {}
