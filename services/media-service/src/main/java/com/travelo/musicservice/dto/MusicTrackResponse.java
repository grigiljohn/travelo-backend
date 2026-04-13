package com.travelo.musicservice.dto;

import java.util.UUID;

/**
 * Response DTO for music track
 */
public record MusicTrackResponse(
        UUID id,
        String name,
        String artist,
        String mood,
        Integer durationSeconds,
        String thumbnailUrl,
        String fileUrl,
        Boolean isRecommended
) {
}

