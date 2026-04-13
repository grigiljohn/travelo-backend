package com.travelo.aiorchestrator.dto;

import java.util.List;

public record MusicAutoRecommendRequest(
        String overallMood,
        String tempo,
        List<String> dominantColors,
        Long durationMs
) {
}

