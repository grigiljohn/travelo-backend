package com.travelo.musicservice.dto;

import java.util.UUID;

/**
 * Response after uploading a track (and optional thumbnail) to S3 and saving metadata.
 */
public record MusicUploadResponse(
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
