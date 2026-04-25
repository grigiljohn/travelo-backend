package com.travelo.admin.media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AdminImageUploadResult(
        @JsonProperty("media_id") UUID mediaId,
        @JsonProperty("download_url") String downloadUrl,
        @JsonProperty("storage_key") String storageKey
) {
}
