package com.travelo.postservice.client.dto;

import java.util.UUID;

/**
 * Response from media-service direct upload (local storage).
 */
public record DirectUploadResponse(
        UUID mediaId,
        String downloadUrl,
        String storageKey
) {}
