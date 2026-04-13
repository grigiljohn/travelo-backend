package com.travelo.mediaservice.dto;

import java.time.Instant;

public record PresignedUploadResponse(
        String fileKey,
        String uploadUrl,
        String downloadUrl,
        Instant expiresAt
) {
}

