package com.travelo.admin.music.dto;

import java.util.UUID;

/**
 * Mirrors media-service {@code MusicTrackResponse} JSON for deserialization.
 */
public record MusicTrackItem(
        UUID id,
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
