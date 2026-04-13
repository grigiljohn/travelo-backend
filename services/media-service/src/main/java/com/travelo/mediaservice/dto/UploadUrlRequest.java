package com.travelo.mediaservice.dto;

import com.travelo.mediaservice.entity.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

/**
 * Request for creating a presigned upload URL.
 * Matches the spec: POST /v1/media/upload-url
 */
public record UploadUrlRequest(
        @NotNull UUID ownerId,
        @NotBlank String filename,
        @NotBlank String mimeType,
        @Positive Long sizeBytes,
        @NotNull MediaType mediaType,
        Boolean resumable,  // optional: request multipart/tus style
        List<String> tags  // optional: e.g., ["post:1234","profile_photo"]
) {
}

