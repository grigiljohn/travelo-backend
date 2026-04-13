package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for confirming upload completion.
 */
public record CompleteUploadRequest(
    @NotBlank(message = "ETag is required")
    String etag,

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    @JsonProperty("size_bytes")
    Long sizeBytes
) {
}

