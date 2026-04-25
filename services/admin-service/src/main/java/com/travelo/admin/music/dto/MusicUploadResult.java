package com.travelo.admin.music.dto;

import java.util.UUID;

/**
 * Mirrors media-service {@code MusicUploadResponse} JSON.
 */
public record MusicUploadResult(
        UUID id,
        String fileKey,
        String thumbnailKey,
        String name,
        String artist,
        String mood,
        String genre,
        Integer bpm,
        String description,
        Integer durationSeconds,
        String thumbnailUrl,
        String fileUrl,
        Boolean isRecommended
) {
}
