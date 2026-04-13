package com.travelo.mediaservice.dto;

import java.util.UUID;

/**
 * Response DTO for processed media operations (trim, crop, rotate).
 */
public record ProcessedMediaResponse(
    UUID processedMediaId,
    String storageKey,
    String downloadUrl,
    String message
) {}

