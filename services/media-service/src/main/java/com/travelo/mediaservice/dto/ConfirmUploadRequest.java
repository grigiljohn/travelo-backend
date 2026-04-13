package com.travelo.mediaservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request to confirm upload completion.
 * Used when client explicitly notifies after upload (optional if S3 events are used).
 */
public record ConfirmUploadRequest(
        @NotBlank String etag,  // S3 ETag
        @NotNull @Positive Long sizeBytes
) {
}

