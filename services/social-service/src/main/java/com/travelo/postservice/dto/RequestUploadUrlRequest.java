package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Request DTO for requesting presigned upload URLs.
 */
public record RequestUploadUrlRequest(
    @NotNull(message = "Owner ID is required")
    @JsonProperty("owner_id")
    UUID ownerId,

    @NotBlank(message = "Filename is required")
    String filename,

    @NotBlank(message = "MIME type is required")
    @JsonProperty("mime_type")
    String mimeType,

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    @JsonProperty("size_bytes")
    Long sizeBytes,

    @NotBlank(message = "Media type is required")
    @Pattern(regexp = "image|video|audio|other", flags = Pattern.Flag.CASE_INSENSITIVE, 
             message = "Media type must be one of: image, video, audio, other")
    @JsonProperty("media_type")
    String mediaType,

    @JsonProperty("resumable")
    Boolean resumable
) {
}

