package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Request DTO for media items in posts.
 * Supports both media-service media IDs (preferred) and legacy URLs (for backward compatibility).
 */
public record MediaItemRequest(
    // New: media-service media ID (preferred)
    @JsonProperty("media_id")
    UUID mediaId,

    // Legacy: direct URL (for backward compatibility)
    String url,

    @NotBlank(message = "Media type is required")
    @Pattern(regexp = "image|video", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Media type must be image or video")
    String type,

    Integer position,    // Order in carousel (0-based)

    @JsonProperty("thumbnail_url")
    String thumbnailUrl, // For videos (cached, optional if media_id is provided)

    Integer duration,    // For videos (seconds, cached, optional if media_id is provided)

    Integer width,       // Cached, optional if media_id is provided

    Integer height       // Cached, optional if media_id is provided
) {
    /**
     * Check if this uses the new media-service ID format.
     */
    public boolean usesMediaService() {
        return mediaId != null;
    }
}

