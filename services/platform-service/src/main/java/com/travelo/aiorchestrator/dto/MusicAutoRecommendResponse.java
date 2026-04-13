package com.travelo.aiorchestrator.dto;

public record MusicAutoRecommendResponse(
        String id,
        String displayName,
        String artist,
        String mood,
        Integer bpm,
        Long durationMs,
        String previewUrl,
        String thumbnailUrl
) {
}

