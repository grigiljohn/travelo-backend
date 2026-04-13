package com.travelo.mediaservice.dto;

import java.util.UUID;

/**
 * Response for direct file upload (local storage).
 */
public record DirectUploadResponse(
        UUID mediaId,
        String downloadUrl,
        String storageKey
) {}
