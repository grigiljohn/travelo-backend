package com.travelo.mediaservice.dto;

import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaStatus;
import com.travelo.mediaservice.entity.MediaType;
import com.travelo.mediaservice.entity.MediaVariant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for MediaFile entity.
 * Updated to match the new entity structure.
 */
public record MediaFileResponse(
        UUID id,
        UUID ownerId,
        MediaType mediaType,
        String mimeType,
        String filename,
        Long sizeBytes,
        String storageKey,
        String storageBucket,
        MediaStatus state,
        String safetyStatus,
        List<MediaVariant> variants,
        Map<String, Object> meta,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static MediaFileResponse fromEntity(MediaFile file) {
        return new MediaFileResponse(
                file.getId(),
                file.getOwnerId(),
                file.getMediaType(),
                file.getMimeType(),
                file.getFilename(),
                file.getSizeBytes(),
                file.getStorageKey(),
                file.getStorageBucket(),
                file.getState(),
                file.getSafetyStatus(),
                file.getVariants(),
                file.getMeta(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }
}
