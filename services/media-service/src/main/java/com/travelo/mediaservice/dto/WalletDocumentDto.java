package com.travelo.mediaservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WalletDocumentDto(
        @JsonProperty("media_id") UUID mediaId,
        @JsonProperty("owner_id") UUID ownerId,
        @JsonProperty("category") String category,
        @JsonProperty("filename") String filename,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("size_bytes") Long sizeBytes,
        @JsonProperty("download_url") String downloadUrl,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
}
