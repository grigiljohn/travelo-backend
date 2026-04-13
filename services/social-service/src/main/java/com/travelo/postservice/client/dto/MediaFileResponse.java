package com.travelo.postservice.client.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MediaFileResponse(
        UUID id,
        UUID ownerId,
        String mediaType,
        String mimeType,
        String filename,
        Long sizeBytes,
        String storageKey,
        String storageBucket,
        String state,
        String safetyStatus,
        List<VariantInfo> variants,
        Map<String, Object> meta,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record VariantInfo(
            String name,
            String key,
            String mime,
            Integer width,
            Integer height,
            Integer bitrate,
            Double duration,
            String signedUrl
    ) {
    }
}

