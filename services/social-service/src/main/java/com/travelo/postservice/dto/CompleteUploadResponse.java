package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Response DTO for upload completion confirmation.
 */
public record CompleteUploadResponse(
    @JsonProperty("media_id")
    UUID mediaId,

    String status,

    @JsonProperty("download_url")
    String downloadUrl
) {
}

